// Ambient declarations for CSS imports handled by Metro/Expo.
declare module "*.css";
declare module "*.module.css" {
  const classes: { readonly [key: string]: string };
  export default classes;
}
