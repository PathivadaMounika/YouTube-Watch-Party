// The YouTube IFrame API loads asynchronously and calls a *global*
// callback (window.onYouTubeIframeAPIReady) when ready - not a promise.
// This wraps that in a promise, and makes sure we only inject the
// <script> tag once even if multiple components ask for it.

let apiReadyPromise = null;

export function loadYouTubeIframeApi() {
  if (apiReadyPromise) return apiReadyPromise;

  apiReadyPromise = new Promise((resolve) => {
    if (window.YT && window.YT.Player) {
      resolve(window.YT);
      return;
    }

    const previousCallback = window.onYouTubeIframeAPIReady;
    window.onYouTubeIframeAPIReady = () => {
      previousCallback?.();
      resolve(window.YT);
    };

    const script = document.createElement('script');
    script.src = 'https://www.youtube.com/iframe_api';
    document.head.appendChild(script);
  });

  return apiReadyPromise;
}
