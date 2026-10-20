const checkEndpoints = async () => {
  const endpoints = [
    "/api/csrf",
    "/api/v1/csrf",
    "/sanctum/csrf-cookie",
    "/api/checkout/v1/plan/401",
    "/api/checkout/v1/plan?plan_id=401",
    "/api/checkout/v1/plan-pricing/516",
    "/api/checkout/v1/config"
  ];
  for (const ep of endpoints) {
    const res = await fetch("https://pr4156-signupcheck.ezysites.com" + ep, {
      headers: {
        "ECOM_HOST": "pr4156-signupcheck.ezysites.com"
      }
    });
    console.log(ep, res.status, res.headers.get("set-cookie") || "no cookie");
  }
};
checkEndpoints();
