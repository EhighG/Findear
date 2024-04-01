/** @type {import('tailwindcss').Config} */
const colors = require("tailwindcss/colors");

export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
    "node_modules/flowbite-react/lib/esm/**/*.js",
  ],
  darkMode: "class",
  theme: {
    extend: {},
    // that is animation class
    animation: {
      fade: "fadeOut 0.5s ease-in-out",
    },

    // that is actual animation
    keyframes: (theme) => ({
      fadeOut: {
        "0%": { opacity: theme("opacity.0") },
        "100%": { backgroundColor: theme("opacity.100") },
      },
    }),

    colors: {
      A706Blue: "#F5F7FF",
      A706Blue2: "#E6EBFF",
      A706Blue3: "#AAB9FF",
      A706SubBlue: "#6F87FF",
      A706CheryBlue: "#5471FF",
      A706Yellow: "#FFD833",
      A706Red: "#F25555",
      A706LightGrey: "#F5F5F5",
      A706LightGrey2: "#DBDCDC",
      A706Grey: "#B5B7BA",
      A706Grey2: "#909398",
      A706DarkGrey1: "#5C5F66",
      A706DarkGrey2: "#292C34",
      A706SlateGrey: "#5D636C",
      A706Green: "#134F2C",
      A706Dark: "#121212",
      cyan: colors.cyan,
    },
  },
  plugins: [
    require("flowbite/plugin"),
    require("daisyui"),
    require("tailwind-scrollbar-hide"),
  ],
};
