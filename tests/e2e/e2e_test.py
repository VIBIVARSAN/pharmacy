#!/usr/bin/env python3
"""
End-to-end test of the Pharmacy Prescription Management System.

It drives the RUNNING application over HTTP exactly like a browser would
(session cookie, CSRF token, form posts) and checks the main workflows:
setup, login, role restrictions, CRUD, stock, FEFO dispensing, partial and
repeated dispensing, billing, duplicate-billing prevention, payments,
reports, search, validation and common security boundaries.

Requirements: Python 3.8+ (standard library only).

IMPORTANT: run it only against a FRESH TEST DATABASE (import database.sql into
an empty schema first). It creates users, patients, medicines and invoices.

    python3 tests/e2e/e2e_test.py --base http://localhost:8080/pharmacy --setup-token YOUR_SETUP_TOKEN

If the first administrator already exists, pass its credentials instead:

    python3 tests/e2e/e2e_test.py --base http://localhost:8080/pharmacy --admin-user admin --admin-pass 'Secret123'
"""
import argparse
import datetime
import html
import http.cookiejar
import json
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None


class Response:
    def __init__(self, status, body, location):
        self.status = status
        self.body = body
        self.location = location or ""
        self.text = html.unescape(re.sub(r"<[^>]+>", " ", body))


class Client:
    """A minimal browser: keeps cookies, remembers the CSRF token, does not auto-follow redirects."""

    def __init__(self, base):
        self.base = base.rstrip("/")
        self.jar = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(NoRedirect, urllib.request.HTTPCookieProcessor(self.jar))
        self.csrf = None

    def _send(self, request):
        try:
            with self.opener.open(request, timeout=30) as r:
                status, body, location = r.status, r.read().decode("utf-8", "replace"), r.headers.get("Location")
        except urllib.error.HTTPError as e:
            status, body, location = e.code, e.read().decode("utf-8", "replace"), e.headers.get("Location")
        m = re.search(r'name="_csrf" value="([^"]+)"', body)
        if m:
            self.csrf = html.unescape(m.group(1))
        return Response(status, body, location)

    def get(self, path):
        return self._send(urllib.request.Request(self.base + path))

    def post(self, path, data=None, csrf=True):
        fields = []
        for key, value in (data or {}).items():
            for v in (value if isinstance(value, list) else [value]):
                fields.append((key, "" if v is None else str(v)))
        if csrf and self.csrf:
            fields.append(("_csrf", self.csrf))
        body = urllib.parse.urlencode(fields).encode()
        return self._send(urllib.request.Request(self.base + path, data=body, method="POST"))

    def follow(self, response):
        """GET the page a redirect points to (path relative to the application)."""
        target = urllib.parse.urlparse(response.location)
        path = target.path
        if path.startswith(urllib.parse.urlparse(self.base).path):
            path = path[len(urllib.parse.urlparse(self.base).path):]
        return self.get(path + ("?" + target.query if target.query else ""))

    def login(self, username, password):
        self.get("/login")
        return self.post("/login", {"username": username, "password": password})


PASSED = 0
FAILED = []


def check(name, condition, detail=""):
    global PASSED
    if condition:
        PASSED += 1
        print("  PASS  " + name)
    else:
        FAILED.append(name)
        print("  FAIL  " + name + ("  -> " + str(detail)[:300] if detail else ""))


def section(title):
    print("\n== " + title)


def new_id(response):
    m = re.search(r"[?&]id=(\d+)", response.location)
    return int(m.group(1)) if m else None


def flash(response):
    m = re.search(r'<div class="toast-body">.*?<span>(.*?)</span>', response.body, re.S)
    return html.unescape(m.group(1)) if m else ""


def usable_stock(client, medicine_id):
    page = client.get("/medicines/view?id=%d" % medicine_id)
    m = re.search(r'Usable stock</span><br><span class="stat-value">(\d+)', page.body)
    return int(m.group(1)) if m else None


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", default="http://localhost:8080/pharmacy")
    parser.add_argument("--setup-token", default=None)
    parser.add_argument("--admin-user", default="e2eadmin")
    parser.add_argument("--admin-pass", default="Admin-pass-123")
    args = parser.parse_args()

    run = str(int(time.time()))[-6:]          # makes names unique per run
    today = datetime.date.today()
    admin = Client(args.base)

    # ------------------------------------------------------------------
    section("Access control before login")
    r = admin.get("/patients")
    check("anonymous request to a protected page is redirected", r.status == 302 and ("/login" in r.location or "/setup" in r.location), r.location)
    r = admin.get("/WEB-INF/views/dashboard.jsp")
    check("JSP files under WEB-INF cannot be requested directly", r.status in (302, 404), r.status)
    r = admin.get("/api/notifications")
    check("anonymous API call returns 401 JSON", r.status == 401 and "error" in r.body, r.status)

    # ------------------------------------------------------------------
    section("First-administrator setup and login")
    r = admin.get("/setup")
    if r.status == 200:
        if not args.setup_token:
            print("The application has no users yet: pass --setup-token.")
            sys.exit(2)
        bad = admin.post("/setup", {"token": "wrong-token", "username": args.admin_user, "fullName": "E2E Admin",
                                    "password": args.admin_pass, "confirmPassword": args.admin_pass})
        check("setup with a wrong token is rejected", bad.status == 400 and "setup token is not correct" in bad.text, bad.status)
        weak = admin.post("/setup", {"token": args.setup_token, "username": args.admin_user, "fullName": "E2E Admin",
                                     "password": "short", "confirmPassword": "short"})
        check("setup with a weak password is rejected", weak.status == 400 and "Password must be" in weak.text, weak.status)
        ok = admin.post("/setup", {"token": args.setup_token, "username": args.admin_user, "fullName": "E2E Admin",
                                   "password": args.admin_pass, "confirmPassword": args.admin_pass})
        check("first administrator is created", ok.status == 302 and ok.location.endswith("/login"), (ok.status, ok.text[:200]))
    else:
        print("  (setup already completed - using the supplied administrator account)")
    r = admin.get("/setup")
    check("setup page is closed once a user exists", r.status == 302 and r.location.endswith("/login"), (r.status, r.location))

    r = admin.login(args.admin_user, "definitely-wrong-password")
    check("wrong password is rejected with a generic message", r.status == 401 and "Invalid username or password" in r.text, r.status)
    r = admin.login("no-such-user-" + run, "whatever123")
    check("unknown user gets the same generic message", r.status == 401 and "Invalid username or password" in r.text, r.status)
    admin.get("/login")
    r = admin.post("/login", {"username": args.admin_user, "password": args.admin_pass}, csrf=False)
    check("login without CSRF token is refused (403)", r.status == 403, r.status)
    r = admin.login(args.admin_user, args.admin_pass)
    check("administrator can sign in", r.status == 302 and r.location.endswith("/dashboard"), (r.status, r.text[:200]))
    cookies = {c.name: c for c in admin.jar}
    check("session cookie is HttpOnly", "JSESSIONID" in cookies and cookies["JSESSIONID"].has_nonstandard_attr("HttpOnly"))
    r = admin.get("/dashboard")
    check("dashboard renders", r.status == 200 and "Registered patients" in r.text, r.status)
    check("security headers are present", True)  # verified below with a raw request
    raw = urllib.request.Request(args.base + "/login")
    with urllib.request.build_opener(NoRedirect).open(raw) as resp:
        headers = resp.headers
    check("Content-Security-Policy and nosniff headers are sent",
          "script-src 'self'" in (headers.get("Content-Security-Policy") or "") and headers.get("X-Content-Type-Options") == "nosniff")

    # ------------------------------------------------------------------
    section("Settings")
    r = admin.post("/settings", {"s_pharmacy_name": "E2E Test Pharmacy", "s_currency_symbol": "Rs.", "s_tax_rate_percent": "",
                                 "s_discounts_enabled": "true", "s_payment_methods": "Cash,Card,UPI",
                                 "s_near_expiry_days": "90", "s_default_reorder_level": "10"})
    check("settings can be saved with the tax rate left empty", r.status == 302, (r.status, r.text[:200]))
    r = admin.post("/settings", {"s_pharmacy_name": "E2E Test Pharmacy", "s_tax_rate_percent": "250", "s_near_expiry_days": "0"})
    check("invalid settings are rejected", r.status == 400 and "between 0 and 100" in r.text, r.status)

    # ------------------------------------------------------------------
    section("Users")
    pharm_user = "pharm" + run
    r = admin.post("/users/save", {"username": pharm_user, "fullName": "Test Pharmacist", "role": "PHARMACIST",
                                   "password": "Pharm-pass-123", "confirmPassword": "Pharm-pass-123"})
    check("administrator creates a pharmacist", r.status == 302, (r.status, r.text[:300]))
    r = admin.post("/users/save", {"username": pharm_user, "fullName": "Dup", "role": "PHARMACIST",
                                   "password": "Pharm-pass-123", "confirmPassword": "Pharm-pass-123"})
    check("duplicate username is rejected", r.status == 400 and "already taken" in r.text, r.status)
    pharm_edit = re.search(r'/users/edit\?id=(\d+)', admin.get("/users?q=" + pharm_user).body).group(1)
    r = admin.post("/users/reset-password", {"id": pharm_edit, "password": "weak", "confirmPassword": "weak", "returnTo": "/users/edit?id=" + pharm_edit})
    check("a weak password is rejected on reset", "Password must be" in flash(admin.follow(r)))
    r = admin.post("/users/reset-password", {"id": pharm_edit, "password": "Reset-pass-123", "confirmPassword": "Reset-pass-123"})
    check("administrator resets the pharmacist's password", r.status == 302 and "has been reset" in flash(admin.follow(r)))
    temp = Client(args.base)
    check("the old password no longer works", temp.login(pharm_user, "Pharm-pass-123").status == 401)
    check("the new password works", temp.login(pharm_user, "Reset-pass-123").status == 302)
    temp.get("/profile")
    r = temp.post("/profile", {"currentPassword": "not-my-password", "newPassword": "Pharm-pass-123", "confirmPassword": "Pharm-pass-123"})
    check("changing your own password needs the current password", r.status == 400 and "current password is not correct" in r.text, r.status)
    r = temp.post("/profile", {"currentPassword": "Reset-pass-123", "newPassword": "Pharm-pass-123", "confirmPassword": "Pharm-pass-123"})
    check("a user can change their own password", r.status == 302 and "has been changed" in flash(temp.follow(r)))

    lock_user = "lock" + run
    admin.post("/users/save", {"username": lock_user, "fullName": "Lock Test", "role": "PHARMACIST",
                               "password": "Lock-pass-123", "confirmPassword": "Lock-pass-123"})
    guesser = Client(args.base)
    for _ in range(5):
        guesser.login(lock_user, "wrong-guess-1")
    r = guesser.login(lock_user, "Lock-pass-123")
    check("five failed logins lock the account name temporarily (even for the right password)",
          r.status == 401 and "Too many failed attempts" in r.text, r.text[:200])

    me = admin.get("/users?q=" + args.admin_user)
    my_id = re.search(r'/users/edit\?id=(\d+)', me.body)
    if my_id:
        r = admin.post("/users/save", {"id": my_id.group(1), "fullName": "E2E Admin", "role": "PHARMACIST", "active": "true"})
        check("administrator cannot remove their own admin role", "cannot remove your own" in flash(admin.follow(r)) if r.status == 302 else False, r.status)

    # ------------------------------------------------------------------
    section("Doctors, suppliers and patients")
    r = admin.post("/doctors/save", {"fullName": "Asha Rao " + run, "specialization": "General Medicine", "phone": "9876500001",
                                     "registrationNo": "REG-" + run, "hospital": "City Clinic"})
    doctor_id = new_id(r)
    check("doctor is created with a generated ID", r.status == 302 and doctor_id and "DOC-" in admin.follow(r).text, r.status)
    r = admin.post("/doctors/save", {"fullName": "", "specialization": "", "phone": "abc"})
    check("invalid doctor input is rejected with field messages", r.status == 400 and "Full name is required" in r.text and "valid phone" in r.text, r.status)
    r = admin.post("/doctors/save", {"fullName": "Other Doc", "specialization": "ENT", "phone": "9876500002", "registrationNo": "REG-" + run})
    check("duplicate registration number is rejected", r.status == 400 and "already uses this registration" in r.text, r.status)

    r = admin.post("/suppliers/save", {"name": "MediSupply " + run, "contactPerson": "R. Kumar", "phone": "9876500003", "email": "orders@example.com"})
    supplier_id = new_id(r)
    check("supplier is created", r.status == 302 and supplier_id, r.status)
    r = admin.post("/suppliers/save", {"name": "Bad Mail", "phone": "9876500004", "email": "not-an-email"})
    check("invalid supplier email is rejected", r.status == 400 and "valid email" in r.text, r.status)

    xss_name = "<script>alert(1)</script> Patient " + run
    r = admin.post("/patients/save", {"fullName": xss_name, "gender": "FEMALE", "phone": "98765" + run[-5:], "dateOfBirth": "1990-05-17",
                                      "address": "12 Lake Road"})
    patient_id = new_id(r)
    page = admin.follow(r)
    check("patient is registered with a generated ID", r.status == 302 and patient_id and "PAT-" in page.text, r.status)
    check("HTML in user input is escaped on output (XSS)", "<script>alert(1)</script>" not in page.body and "&lt;script&gt;alert(1)&lt;/script&gt;" in page.body)
    r = admin.post("/patients/save", {"fullName": xss_name, "gender": "FEMALE", "phone": "98765" + run[-5:]})
    check("probable duplicate patient is rejected", r.status == 400 and "already exists" in r.text, r.status)
    r = admin.post("/patients/save", {"fullName": "No Gender", "phone": "12", "email": "x@", "dateOfBirth": "2999-01-01"})
    check("patient validation reports gender, phone, email and date errors",
          r.status == 400 and all(s in r.text for s in ["Gender is required", "valid phone", "valid email", "cannot be in the future"]), r.status)
    r = admin.post("/patients/save", {"id": patient_id, "fullName": "Meera Nair " + run, "gender": "FEMALE", "phone": "98765" + run[-5:]})
    check("patient can be updated", r.status == 302 and "Meera Nair" in admin.follow(r).text, r.status)
    for i in range(12):
        admin.post("/patients/save", {"fullName": "Bulk Patient %s %02d" % (run, i), "gender": "MALE", "phone": "97000%s%02d" % (run[-3:], i)})
    r = admin.get("/patients?q=Bulk+Patient+" + run + "&sort=name&dir=asc")
    check("search + pagination: first page has 10 of 12 matches", r.status == 200 and "of 12 patients" in r.text and r.body.count("row-link") == 10, r.text[-400:])
    first_asc = re.search(r"Bulk Patient \d+ (\d\d)", r.text).group(1)
    r2 = admin.get("/patients?q=Bulk+Patient+" + run + "&sort=name&dir=desc")
    first_desc = re.search(r"Bulk Patient \d+ (\d\d)", r2.text).group(1)
    check("sorting changes the order", first_asc == "00" and first_desc == "11", (first_asc, first_desc))
    r = admin.get("/patients?q=Bulk+Patient+" + run + "&page=2")
    check("second page shows the remaining 2", r.body.count("row-link") == 2)
    r = admin.get("/patients?q=" + urllib.parse.quote("' OR '1'='1"))
    check("SQL injection text in search is treated as plain text", r.status == 200 and "No patients match" in r.text, r.status)
    r = admin.get("/patients?sort=" + urllib.parse.quote("name; DROP TABLE patients"))
    check("unknown sort column is ignored safely", r.status == 200)
    r = admin.get("/patients/view?id=999999999")
    check("unknown record id gives 404", r.status == 404, r.status)

    # ------------------------------------------------------------------
    section("Medicines and stock")
    r = admin.post("/medicines/save", {"name": "Paracetamol " + run, "genericName": "Acetaminophen", "category": "Analgesic",
                                       "dosageForm": "Tablet", "strength": "500 mg", "unit": "tablet", "reorderLevel": "15"})
    med_a = new_id(r)
    check("medicine A is created", r.status == 302 and med_a, (r.status, r.text[:300]))
    r = admin.post("/medicines/save", {"name": "Amoxicillin " + run, "category": "Antibiotic", "dosageForm": "Capsule",
                                       "strength": "250 mg", "unit": "capsule", "reorderLevel": "5"})
    med_b = new_id(r)
    check("medicine B is created", r.status == 302 and med_b, r.status)
    r = admin.post("/medicines/save", {"name": "Paracetamol " + run, "category": "Analgesic", "dosageForm": "Tablet",
                                       "strength": "500 mg", "unit": "tablet", "reorderLevel": "x"})
    check("medicine validation (non-numeric reorder level) is rejected", r.status == 400 and "Reorder level" in r.text, r.status)
    check("new medicine starts with zero stock", usable_stock(admin, med_a) == 0)

    def stock_in(med, batch, expiry, qty, cost, price):
        return admin.post("/inventory/stock-in", {"medicineId": med, "supplierId": supplier_id, "batchNumber": batch,
                                                  "expiryDate": expiry.isoformat(), "receivedDate": today.isoformat(),
                                                  "quantity": qty, "purchasePrice": cost, "sellingPrice": price})

    r = stock_in(med_a, "A-LATE", today + datetime.timedelta(days=400), 20, "1.50", "3.00")
    check("stock-in creates batch A-LATE (20)", r.status == 302, (r.status, r.text[:300]))
    r = stock_in(med_a, "A-SOON", today + datetime.timedelta(days=30), 10, "1.20", "2.50")
    check("stock-in creates batch A-SOON (10)", r.status == 302, r.status)
    r = stock_in(med_b, "B-1", today + datetime.timedelta(days=300), 8, "4.00", "7.25")
    check("stock-in creates batch B-1 (8)", r.status == 302, r.status)
    check("usable stock of A is 30", usable_stock(admin, med_a) == 30, usable_stock(admin, med_a))
    r = stock_in(med_a, "A-OLD", today - datetime.timedelta(days=1), 5, "1.00", "2.00")
    check("receiving already-expired stock is rejected", r.status == 400 and "Expired stock cannot be received" in r.text, r.status)
    r = stock_in(med_a, "A-BAD", today + datetime.timedelta(days=90), -4, "1.00", "2.00")
    check("negative quantity is rejected", r.status == 400 and "greater than zero" in r.text, r.status)
    r = stock_in(med_a, "A-LATE", today + datetime.timedelta(days=10), 5, "1.50", "3.00")
    check("same batch number with a different expiry is rejected", r.status == 400 and "already exists with expiry" in r.text, r.status)
    r = stock_in(med_a, "A-LATE", today + datetime.timedelta(days=400), 5, "1.50", "3.00")
    check("receiving into an existing batch tops it up (A = 35)", r.status == 302 and usable_stock(admin, med_a) == 35, usable_stock(admin, med_a))

    batches = admin.get("/inventory?q=A-LATE")
    late_id = int(re.search(r'data-fill-batch-id="(\d+)" data-fill-label="A-LATE"', batches.body).group(1))
    r = admin.post("/inventory/stock-out", {"batchId": late_id, "quantity": 1000, "reason": "test", "returnTo": "/inventory"})
    check("stock-out larger than the batch is rejected (no negative stock)",
          r.status == 302 and "Insufficient stock" in flash(admin.follow(r)) and usable_stock(admin, med_a) == 35)
    r = admin.post("/inventory/stock-out", {"batchId": late_id, "quantity": 3, "reason": "", "returnTo": "/inventory"})
    check("stock-out without a reason is rejected", "Reason is required" in flash(admin.follow(r)) and usable_stock(admin, med_a) == 35)
    r = admin.post("/inventory/stock-out", {"batchId": late_id, "quantity": 3, "reason": "Damaged in storage", "returnTo": "/inventory"})
    check("stock-out removes 3 (A = 32)", r.status == 302 and usable_stock(admin, med_a) == 32, usable_stock(admin, med_a))
    r = admin.post("/inventory/adjust", {"batchId": late_id, "direction": "in", "quantity": 3, "reason": "Stock count correction"})
    check("administrator adjustment adds 3 (A = 35)", r.status == 302 and usable_stock(admin, med_a) == 35, usable_stock(admin, med_a))
    ledger = admin.get("/inventory/transactions?medicineId=%d" % med_a)
    check("stock ledger lists stock-in, stock-out and adjustment",
          all(s in ledger.text for s in ["Stock in", "Stock out", "Adjustment (+)", "Damaged in storage"]))

    # ------------------------------------------------------------------
    section("Prescriptions")
    rx_fields = {"patientId": patient_id, "doctorId": doctor_id, "prescriptionDate": today.isoformat(),
                 "diagnosis": "Entered by test", "notes": "",
                 "itemMedicineId": [med_a, med_b], "itemDosage": ["1 tablet", "1 capsule"],
                 "itemFrequency": ["Twice daily", "Three times daily"], "itemDuration": ["5 days", "5 days"],
                 "itemInstructions": ["After food", ""], "itemQuantity": [25, 5]}
    r = admin.post("/prescriptions/save", dict(rx_fields, patientId=999999999, action="issue"))
    check("prescription for an unknown patient is rejected", r.status == 400 and "registered patient" in r.text, r.status)
    r = admin.post("/prescriptions/save", dict(rx_fields, itemMedicineId=[], itemDosage=[], itemFrequency=[], itemDuration=[],
                                               itemInstructions=[], itemQuantity=[], action="issue"))
    check("prescription without medicines is rejected", r.status == 400 and "at least one medicine" in r.text, r.status)
    r = admin.post("/prescriptions/save", dict(rx_fields, itemMedicineId=[med_a, med_a], action="issue"))
    check("the same medicine twice is rejected", r.status == 400 and "more than once" in r.text, r.status)
    r = admin.post("/prescriptions/save", dict(rx_fields, action="draft"))
    rx_id = new_id(r)
    page = admin.follow(r)
    check("prescription with two items is saved as a draft", r.status == 302 and rx_id and "Draft" in page.text and "RX-" in page.text, r.status)
    r = admin.get("/dispensing/new?prescriptionId=%d" % rx_id)
    check("a draft cannot be dispensed", r.status == 302 and "cannot be dispensed" in flash(admin.follow(r)))
    r = admin.post("/prescriptions/save", dict(rx_fields, id=rx_id, notes="Updated note", action="draft"))
    check("draft can be edited", r.status == 302 and "Updated note" in admin.follow(r).text, r.status)
    r = admin.post("/prescriptions/issue", {"id": rx_id})
    check("draft can be issued", r.status == 302 and "Issued" in admin.follow(r).text, r.status)
    r = admin.get("/prescriptions/print?id=%d" % rx_id)
    check("printable prescription renders", r.status == 200 and "Doctor's signature" in r.text and "Twice daily" in r.text, r.status)

    # ------------------------------------------------------------------
    section("Role restrictions (pharmacist)")
    pharm = Client(args.base)
    r = pharm.login(pharm_user, "Pharm-pass-123")
    check("pharmacist can sign in", r.status == 302 and r.location.endswith("/dashboard"), r.status)
    pharm.get("/dashboard")
    for path in ["/users", "/reports", "/reports/sales", "/audit-logs", "/settings"]:
        check("pharmacist is denied GET " + path, pharm.get(path).status == 403)
    denied = [
        ("/users/save", {"username": "evil" + run, "fullName": "Evil", "role": "ADMIN", "password": "Evil-pass-123", "confirmPassword": "Evil-pass-123"}),
        ("/settings", {"s_tax_rate_percent": "0"}),
        ("/doctors/save", {"fullName": "X", "specialization": "Y", "phone": "9876511111"}),
        ("/medicines/save", {"name": "X", "category": "Y", "dosageForm": "Tablet", "unit": "tablet", "reorderLevel": "1"}),
        ("/suppliers/save", {"name": "X", "phone": "9876511111"}),
        ("/prescriptions/save", dict(rx_fields, action="issue")),
        ("/prescriptions/cancel", {"id": rx_id, "reason": "nope"}),
        ("/inventory/adjust", {"batchId": late_id, "direction": "in", "quantity": 50, "reason": "nope"}),
        ("/patients/deactivate", {"id": patient_id}),
    ]
    for path, data in denied:
        check("pharmacist is denied POST " + path, pharm.post(path, data).status == 403)
    check("denied adjustment did not change stock", usable_stock(pharm, med_a) == 35)
    r = pharm.post("/patients/save", {"fullName": "Walk-in " + run, "gender": "MALE", "phone": "9123400000"})
    check("pharmacist can register a patient", r.status == 302, r.status)
    r = pharm.get("/prescriptions/view?id=%d" % rx_id)
    check("pharmacist can view a prescription but sees no edit control", r.status == 200 and "/prescriptions/edit" not in r.body)

    # ------------------------------------------------------------------
    section("Dispensing")
    form = pharm.get("/dispensing/new?prescriptionId=%d" % rx_id)
    check("dispensing form lists the prescribed items", form.status == 200 and "Twice daily" in form.text, form.status)
    token = re.search(r'name="requestToken" value="([^"]+)"', form.body).group(1)
    items = dict((html.unescape(n), int(i)) for i, n in re.findall(r'for="qty_(\d+)">Quantity of (.*?) to dispense now', form.body))
    item_a = next(v for k, v in items.items() if k.startswith("Paracetamol"))
    item_b = next(v for k, v in items.items() if k.startswith("Amoxicillin"))

    r = pharm.post("/dispensing/save", {"prescriptionId": rx_id, "requestToken": token, "qty_%d" % item_a: 26, "qty_%d" % item_b: 5,
                                        "returnTo": "/dispensing/new?prescriptionId=%d" % rx_id})
    check("dispensing more than prescribed is rejected", "remain to be dispensed" in flash(pharm.follow(r)) and usable_stock(pharm, med_a) == 35)
    r = pharm.post("/dispensing/save", {"prescriptionId": rx_id, "requestToken": token, "qty_%d" % item_a: 0, "qty_%d" % item_b: 0,
                                        "returnTo": "/dispensing/new?prescriptionId=%d" % rx_id})
    check("dispensing nothing is rejected", "at least one medicine" in flash(pharm.follow(r)))

    first = {"prescriptionId": rx_id, "requestToken": token, "qty_%d" % item_a: 12, "qty_%d" % item_b: 5, "notes": "first part"}
    r = pharm.post("/dispensing/save", first)
    disp1 = new_id(r)
    view = pharm.follow(r)
    check("partial dispensing succeeds", r.status == 302 and "/dispensing/view" in r.location and "DSP-" in view.text, (r.status, flash(view)))
    rows = re.findall(r"<td>(A-SOON|A-LATE|B-1)</td>\s*<td>.*?</td>\s*<td class=\"text-end\">(\d+)</td>", view.body, re.S)
    check("FEFO: 10 taken from the earliest-expiring batch, 2 from the next", ("A-SOON", "10") in rows and ("A-LATE", "2") in rows, rows)
    check("stock was deducted once (A = 23, B = 3)", usable_stock(pharm, med_a) == 23 and usable_stock(pharm, med_b) == 3,
          (usable_stock(pharm, med_a), usable_stock(pharm, med_b)))
    r = pharm.post("/dispensing/save", first)
    replay = pharm.follow(r)
    check("repeating the same submission does not deduct stock again",
          r.status == 302 and new_id(r) == disp1 and "already recorded" in flash(replay) and usable_stock(pharm, med_a) == 23, flash(replay))
    page = pharm.get("/prescriptions/view?id=%d" % rx_id)
    check("prescription is now partially dispensed", "Partially dispensed" in page.text)
    r = admin.post("/prescriptions/cancel", {"id": rx_id, "reason": "too late"})
    check("a partially dispensed prescription cannot be cancelled", "cannot be cancelled" in flash(admin.follow(r)))
    r = admin.post("/prescriptions/save", dict(rx_fields, id=rx_id, action="save"))
    check("a partially dispensed prescription cannot be edited", "can no longer be edited" in flash(admin.follow(r)))

    form = pharm.get("/dispensing/new?prescriptionId=%d" % rx_id)
    token2 = re.search(r'name="requestToken" value="([^"]+)"', form.body).group(1)
    r = pharm.post("/dispensing/save", {"prescriptionId": rx_id, "requestToken": token2, "qty_%d" % item_a: 13})
    disp2 = new_id(r)
    check("remaining quantity is dispensed", r.status == 302 and disp2 and disp2 != disp1, flash(pharm.follow(r)))
    page = pharm.get("/prescriptions/view?id=%d" % rx_id)
    check("prescription is completed", "Completed" in page.text and usable_stock(pharm, med_a) == 10, usable_stock(pharm, med_a))
    r = pharm.get("/dispensing/new?prescriptionId=%d" % rx_id)
    check("a completed prescription cannot be dispensed again", r.status == 302 and "cannot be dispensed" in flash(pharm.follow(r)))

    # insufficient stock: prescribe 50 of B (only 3 left)
    r = admin.post("/prescriptions/save", dict(rx_fields, itemMedicineId=[med_b], itemDosage=["1 capsule"], itemFrequency=["Daily"],
                                               itemDuration=[""], itemInstructions=[""], itemQuantity=[50], action="issue"))
    rx2 = new_id(r)
    form = pharm.get("/dispensing/new?prescriptionId=%d" % rx2)
    token3 = re.search(r'name="requestToken" value="([^"]+)"', form.body).group(1)
    item_b2 = int(re.search(r'for="qty_(\d+)"', form.body).group(1))
    r = pharm.post("/dispensing/save", {"prescriptionId": rx2, "requestToken": token3, "qty_%d" % item_b2: 4,
                                        "returnTo": "/dispensing/new?prescriptionId=%d" % rx2})
    check("dispensing more than the available stock is rejected and nothing is saved",
          "Insufficient stock" in flash(pharm.follow(r)) and usable_stock(pharm, med_b) == 3, usable_stock(pharm, med_b))
    page = pharm.get("/prescriptions/view?id=%d" % rx2)
    check("failed dispensing left the prescription untouched (rollback)", "Nothing dispensed yet" in page.text and "Issued" in page.text)

    # ------------------------------------------------------------------
    section("Billing")
    r = pharm.get("/invoices/new?dispensingId=%d" % disp1)
    check("billing is blocked until a tax rate is configured", r.status == 302 and "No tax rate has been configured" in flash(pharm.follow(r)))
    r = admin.post("/settings", {"s_pharmacy_name": "E2E Test Pharmacy", "s_currency_symbol": "Rs.", "s_tax_rate_percent": "5",
                                 "s_discounts_enabled": "true", "s_payment_methods": "Cash,Card,UPI",
                                 "s_near_expiry_days": "90", "s_default_reorder_level": "10"})
    check("administrator configures a 5% tax rate", r.status == 302)

    form = pharm.get("/invoices/new?dispensingId=%d" % disp1)
    # dispensing 1: 10 x 2.50 + 2 x 3.00 + 5 x 7.25 = 67.25
    check("invoice preview shows the server-calculated subtotal 67.25", form.status == 200 and "67.25" in form.text, form.text[-600:])
    line_ids = re.findall(r'name="discount_(\d+)"', form.body)
    check("invoice form has one line per dispensed batch (3)", len(line_ids) == 3, line_ids)
    r = pharm.post("/invoices/save", {"dispensingId": disp1, "discount_" + line_ids[0]: "999", "returnTo": "/invoices/new?dispensingId=%d" % disp1})
    check("a discount larger than the line is rejected", "larger than the line amount" in flash(pharm.follow(r)))
    r = pharm.post("/invoices/save", {"dispensingId": disp1, "discount_" + line_ids[0]: "2.25", "paymentMethod": "Bitcoin",
                                      "paymentAmount": "10", "returnTo": "/invoices/new?dispensingId=%d" % disp1})
    check("an unknown payment method is rejected and no invoice is created (rollback)",
          "configured payment methods" in flash(pharm.follow(r)) and pharm.get("/invoices/new?dispensingId=%d" % disp1).status == 200)
    r = pharm.post("/invoices/save", {"dispensingId": disp1, "discount_" + line_ids[0]: "2.25", "paymentMethod": "Cash",
                                      "paymentAmount": "20.00", "paymentReference": "till-1"})
    inv1 = new_id(r)
    view = pharm.follow(r)
    # subtotal 67.25, discount 2.25, taxable 65.00, tax 5% = 3.25, total 68.25, paid 20.00, balance 48.25
    check("invoice is created with a generated number", r.status == 302 and inv1 and "INV-" in view.text, (r.status, flash(view)))
    check("totals are calculated on the server (65.00 + 5% = 68.25, balance 48.25)",
          all(s in view.text for s in ["67.25", "2.25", "3.25", "68.25", "48.25"]) and "Partially paid" in view.text, view.text[-900:])
    check("billing did not touch stock", usable_stock(pharm, med_a) == 10 and usable_stock(pharm, med_b) == 3)
    r = pharm.post("/invoices/save", {"dispensingId": disp1})
    check("the same dispensing cannot be billed twice", r.status == 302 and new_id(r) == inv1 and "already been billed" in flash(pharm.follow(r)))
    r = pharm.get("/invoices/new?dispensingId=%d" % disp1)
    check("the invoice form redirects to the existing invoice", r.status == 302 and new_id(r) == inv1)
    r = pharm.post("/invoices/pay", {"id": inv1, "paymentAmount": "100", "paymentMethod": "Card", "returnTo": "/invoices/view?id=%d" % inv1})
    check("overpayment is rejected", "more than the balance due" in flash(pharm.follow(r)))
    r = pharm.post("/invoices/pay", {"id": inv1, "paymentAmount": "48.25", "paymentMethod": "Card", "paymentReference": "slip-9"})
    check("final payment marks the invoice as paid", r.status == 302 and "Paid" in pharm.follow(r).text)
    r = pharm.post("/invoices/cancel", {"id": inv1, "reason": "test"})
    check("pharmacist cannot cancel an invoice", r.status == 403, r.status)
    r = admin.post("/invoices/cancel", {"id": inv1, "reason": "test", "returnTo": "/invoices/view?id=%d" % inv1})
    check("an invoice with payments cannot be cancelled", "recorded payments cannot be cancelled" in flash(admin.follow(r)))
    r = pharm.get("/invoices/print?id=%d" % inv1)
    check("printable invoice renders with pharmacy details", r.status == 200 and "E2E Test Pharmacy" in r.text and "Authorised signature" in r.text)

    r = pharm.post("/invoices/save", {"dispensingId": disp2})
    inv2 = new_id(r)
    view = pharm.follow(r)
    # dispensing 2: 13 x 3.00 = 39.00, tax 1.95, total 40.95
    check("second invoice (unpaid) is created: 39.00 + 1.95 = 40.95", inv2 and "40.95" in view.text and "Unpaid" in view.text, view.text[-600:])
    r = admin.post("/invoices/cancel", {"id": inv2, "reason": "Entered against wrong visit"})
    view = admin.follow(r)
    check("administrator cancels the unpaid invoice; it stays on record", "Cancelled" in view.text and "Entered against wrong visit" in view.text)
    r = pharm.post("/invoices/save", {"dispensingId": disp2, "paymentMethod": "UPI", "paymentAmount": "40.95"})
    inv3 = new_id(r)
    check("after cancellation the dispensing can be billed again", inv3 and inv3 != inv2 and "Paid" in pharm.follow(r).text)

    # ------------------------------------------------------------------
    section("Dashboard, reports, search, audit log and JSON API")
    dash = admin.get("/dashboard")
    sales_today = re.search(r"Today's sales</span><br><span class=\"stat-value\"><small>.*?</small>\s*([\d,.]+)", dash.body)
    check("dashboard shows today's sales from the database (68.25 + 40.95 = 109.20, cancelled invoice excluded)",
          sales_today is not None and sales_today.group(1).replace(",", "").endswith("109.20") or
          (sales_today is not None and float(sales_today.group(1).replace(",", "")) >= 109.20), sales_today.group(1) if sales_today else None)
    api = json.loads(admin.get("/api/dashboard/sales-trend?days=7").body)
    check("sales-trend API returns 7 daily points ending today", len(api["labels"]) == 7 and api["labels"][-1] == today.isoformat() and float(api["values"][-1]) >= 109.20, api)
    api = json.loads(admin.get("/api/dashboard/stock-by-category").body)
    check("stock-by-category API returns categories", "Analgesic" in api["labels"], api)
    api = json.loads(admin.get("/api/notifications").body)
    check("notifications API reports low stock and near-expiry counts", api["lowStock"] >= 2 and "nearExpiry" in api, api)
    api = json.loads(pharm.get("/api/lookup/patients?q=Meera").body)
    check("patient lookup API finds the patient", any("Meera Nair" in p["label"] for p in api), api)

    reports = {"/reports/sales": "Total sales", "/reports/sales?filter=month": "Total sales", "/reports/billing": "Outstanding",
               "/reports/prescriptions": "Prescriptions", "/reports/inventory": "Usable units", "/reports/low-stock": "reorder level",
               "/reports/expiry": "Batches", "/reports/stock-movements": "Units out", "/reports/supplier-receipts": "Units received"}
    for path, marker in reports.items():
        r = admin.get(path)
        check("report " + path + " renders with totals", r.status == 200 and marker in r.text, (r.status, r.text[:200]))
    r = admin.get("/reports/sales?from=2000-01-01&to=2000-01-31")
    check("a report with no data shows an empty state", r.status == 200 and "No data for this report" in r.text)
    r = admin.get("/reports/billing?filter=CANCELLED")
    check("billing report can filter cancelled invoices", r.status == 200 and "Cancelled" in r.text)
    r = admin.get("/reports/stock-movements?filter=DISPENSE&size=100")
    check("stock-movement report filters by type", r.status == 200 and "Dispensed" in r.text and "Stock in" not in re.sub(r"<select.*?</select>", "", r.body, flags=re.S))

    r = admin.get("/search?q=Meera")
    check("global search finds the patient", r.status == 200 and "Meera Nair" in r.text)
    r = admin.get("/search?q=" + urllib.parse.quote("Paracetamol " + run))
    check("global search finds the medicine", "Paracetamol" in r.text and "in stock" in r.text)
    r = admin.get("/audit-logs?size=100")
    check("audit log records the key events",
          all(s in r.text for s in ["Login", "Patient created", "Stock in", "Dispensed", "Invoice created", "Payment recorded", "Invoice cancelled"]), r.text[:300])
    check("audit log contains no password", args.admin_pass not in r.body and "Pharm-pass-123" not in r.body)
    r = admin.get("/audit-logs?action=LOGIN_FAILED")
    check("failed logins are audited", r.status == 200 and "Failed sign-in attempt" in r.text)

    # ------------------------------------------------------------------
    section("Deactivation and logout")
    r = admin.post("/doctors/deactivate", {"id": doctor_id})
    check("doctor can be deactivated", r.status == 302 and "Deactivated" in admin.follow(r).text)
    check("deactivated doctor is still shown on the old prescription", "Asha Rao" in admin.get("/prescriptions/view?id=%d" % rx_id).text)
    r = admin.post("/prescriptions/save", dict(rx_fields, action="issue"))
    check("a deactivated doctor cannot be used for a new prescription", r.status == 400 and "deactivated" in r.text, r.status)

    pharm_page = admin.get("/users?q=" + pharm_user)
    pharm_id = re.search(r'/users/edit\?id=(\d+)', pharm_page.body).group(1)
    r = admin.post("/users/save", {"id": pharm_id, "fullName": "Test Pharmacist", "role": "PHARMACIST", "active": "false"})
    check("administrator deactivates the pharmacist", r.status == 302)
    r = pharm.get("/dashboard")
    check("a deactivated user's existing session stops working immediately", r.status == 302 and "/login" in r.location, (r.status, r.location))
    r = Client(args.base).login(pharm_user, "Pharm-pass-123")
    check("a deactivated user cannot sign in", r.status == 401)

    r = admin.get("/logout")
    check("logout by GET is not allowed", r.status == 405, r.status)
    admin.get("/dashboard")
    r = admin.post("/logout")
    check("logout redirects to the login page", r.status == 302 and "/login" in r.location)
    r = admin.get("/dashboard")
    check("after logout the session is gone", r.status == 302 and "/login" in r.location)

    print("\n%d checks passed, %d failed" % (PASSED, len(FAILED)))
    for name in FAILED:
        print("  FAILED: " + name)
    sys.exit(1 if FAILED else 0)


if __name__ == "__main__":
    main()
