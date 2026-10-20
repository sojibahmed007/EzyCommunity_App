const inspectScripts = async () => {
  const url = "https://pr4156-signupcheck.ezysites.com/signup?plan_id=401";
  const res = await fetch(url);
  const html = await res.text();
  
  const scriptTags = html.match(/<script[^>]*src="([^"]+)"/g);
  console.log("Script tags:", scriptTags);

  // Check if there are any window or config objects
  const inlineScripts = html.match(/<script[^>]*>([\s\S]*?)<\/script>/g);
  if (inlineScripts) {
    inlineScripts.forEach((s, i) => {
      if (!s.includes('src=')) {
        console.log(`Inline script ${i}:`, s.slice(0, 300));
      }
    });
  }
};
inspectScripts();
