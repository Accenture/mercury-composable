/// <reference types="vite/client" />

/** Allow TypeScript to import *.module.css files as plain style objects. */
declare module '*.module.css' {
  const styles: { readonly [className: string]: string };
  export default styles;
}
