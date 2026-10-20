const test = async () => {
  const url = "https://pr4156-signupcheck.ezysites.com/api/checkout/v1/plan/finalize";
  const headers = {
    "ECOM_HOST": "pr4156-signupcheck.ezysites.com",
    "sec-ch-ua-platform": "\"Android\"",
    "X-XSRF-TOKEN": "e:ak9HPtHZDPEH3zTJpc8g_Hs5xcPV7JaWBGzPlWLNOLpenLUHcYHZyZhxaIO01Nr_VbIui8cd7nnqT7J-bfOjNgfWYPT2kAQ5cBKf9_CuBTQ.bm5EYmtZcEJOdmpQZmVSNA.STwSgV0cEkI6CkgYYT8eCDiaib9phrQOoMu4xIIAoJw",
    "Referer": "https://pr4156-signupcheck.ezysites.com/signup?plan_id=401",
    "User-Agent": "Mozilla/5.0 (Linux; Android 16; Pixel 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/155.0.0.0 Mobile Safari/537.36",
    "Accept": "application/json, text/plain, */*",
    "Content-Type": "application/json"
  };

  // 1. Without password field
  const payloadNoPass = {
    firstName: "Xantha",
    lastName: "Dyer",
    email: "lyje@imti.online",
    company_name: "Barrett Cabrera LLC",
    is_trial: false,
    stripe_setup_intent_id: "seti_1USYqHRos7CuLaXZuAK9fxgs",
    gateway: "stripe",
    plan_price_id: 516,
    coupon: null,
    idempotency_key: "a845e4d7-036f-4da8-a685-11dd59da547d",
    save_card: true
  };

  console.log("--- 1. Testing without password field ---");
  try {
    const res = await fetch(url, {
      method: "POST",
      headers,
      body: JSON.stringify(payloadNoPass)
    });
    console.log("Status:", res.status, res.statusText);
    const body = await res.text();
    console.log("Body:", body);
  } catch (err) {
    console.error("Fetch error:", err);
  }

  // 2. With password = ""
  console.log("\n--- 2. Testing with password = \"\" ---");
  try {
    const res = await fetch(url, {
      method: "POST",
      headers,
      body: JSON.stringify({ ...payloadNoPass, password: "" })
    });
    console.log("Status:", res.status, res.statusText);
    const body = await res.text();
    console.log("Body:", body);
  } catch (err) {
    console.error("Fetch error:", err);
  }

  // 3. With original payload (with password) just to check if token is valid or what happens
  console.log("\n--- 3. Testing with provided original payload ---");
  try {
    const res = await fetch(url, {
      method: "POST",
      headers,
      body: JSON.stringify({ ...payloadNoPass, password: "#Ab12345" })
    });
    console.log("Status:", res.status, res.statusText);
    const body = await res.text();
    console.log("Body:", body);
  } catch (err) {
    console.error("Fetch error:", err);
  }
};

test();
