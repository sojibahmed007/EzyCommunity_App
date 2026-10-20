const getCsrf = async () => {
  const url = "https://pr4156-signupcheck.ezysites.com/signup?plan_id=401";
  const res = await fetch(url, {
    headers: {
      "ECOM_HOST": "pr4156-signupcheck.ezysites.com",
      "User-Agent": "Mozilla/5.0 (Linux; Android 16; Pixel 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/155.0.0.0 Mobile Safari/537.36"
    }
  });

  console.log("Signup GET Status:", res.status);
  console.log("Set-Cookie headers:", res.headers.getSetCookie ? res.headers.getSetCookie() : res.headers.get("set-cookie"));
  const html = await res.text();
  console.log("HTML length:", html.length);
  
  // Look for csrf token in html if any
  const match = html.match(/csrf|xsrf|token/gi);
  console.log("Matches:", match ? match.slice(0, 10) : "none");
};

getCsrf();
