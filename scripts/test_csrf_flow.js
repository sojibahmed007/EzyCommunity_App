const testWithSession = async () => {
  // Step 1: Make a request to get CSRF cookies
  const initRes = await fetch("https://pr4156-signupcheck.ezysites.com/api/csrf", {
    headers: {
      "ECOM_HOST": "pr4156-signupcheck.ezysites.com"
    }
  });

  const rawCookies = initRes.headers.getSetCookie ? initRes.headers.getSetCookie() : [initRes.headers.get("set-cookie")];
  console.log("Raw Set-Cookie headers:", rawCookies);

  let cookieMap = {};
  rawCookies.forEach(cStr => {
    if (!cStr) return;
    const parts = cStr.split(';');
    const [k, v] = parts[0].split('=');
    cookieMap[k.trim()] = v ? v.trim() : '';
  });

  console.log("Cookie map keys:", Object.keys(cookieMap));
  const xsrfTokenCookie = cookieMap['XSRF-TOKEN'];
  const xsrfToken = decodeURIComponent(xsrfTokenCookie);
  console.log("Decoded XSRF-TOKEN:", xsrfToken);

  const cookieHeader = Object.entries(cookieMap).map(([k, v]) => `${k}=${v}`).join('; ');

  const headers = {
    "ECOM_HOST": "pr4156-signupcheck.ezysites.com",
    "sec-ch-ua-platform": "\"Android\"",
    "X-XSRF-TOKEN": xsrfToken,
    "Cookie": cookieHeader,
    "Referer": "https://pr4156-signupcheck.ezysites.com/signup?plan_id=401",
    "User-Agent": "Mozilla/5.0 (Linux; Android 16; Pixel 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/155.0.0.0 Mobile Safari/537.36",
    "Accept": "application/json, text/plain, */*",
    "Content-Type": "application/json"
  };

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

  console.log("\n--- Testing POST /api/checkout/v1/plan/finalize (Without password) ---");
  const postRes = await fetch("https://pr4156-signupcheck.ezysites.com/api/checkout/v1/plan/finalize", {
    method: "POST",
    headers,
    body: JSON.stringify(payloadNoPass)
  });

  console.log("Status:", postRes.status, postRes.statusText);
  const bodyText = await postRes.text();
  console.log("Body:", bodyText);
};

testWithSession();
