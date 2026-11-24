// Give Luna a desktop-width layout while preserving TV-sized native app controls.
(() => {
  if (window !== window.top) return;
  const width = 1280;
  const scale = Math.max(0.25, Math.min(2, screen.width / width));
  const content = 'width=' + width + ', initial-scale=' + scale + ', minimum-scale=' + scale;
  let headObserver;
  function apply() {
    if (!document.head) return false;
    const metas = Array.from(document.head.querySelectorAll('meta[name="viewport"]'));
    if (!metas.length) {
      const meta = document.createElement('meta');
      meta.name = 'viewport';
      document.head.appendChild(meta);
      metas.push(meta);
    }
    // Parsing/hydration can append another viewport after our early-created one.
    for (const meta of metas) if (meta.content !== content) meta.content = content;
    // Frameworks may replace/update their viewport after boot.
    if (!headObserver) {
      headObserver = new MutationObserver(apply);
      headObserver.observe(document.head, {childList:true, subtree:true, attributes:true, attributeFilter:['content','name']});
    }
    return true;
  }
  if (!apply()) {
    const ready = new MutationObserver(() => { if (apply()) ready.disconnect(); });
    ready.observe(document, {childList:true, subtree:true});
  }
})();
