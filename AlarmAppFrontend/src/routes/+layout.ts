/** Client-only, weil der Alarm-Stream und die REST-Aufrufe im Browser laufen. */
export const ssr = false;

/** Kein Vorab-Rendern, damit der Docker-Build als SPA mit Fallback ausgeliefert werden kann. */
export const prerender = false;
