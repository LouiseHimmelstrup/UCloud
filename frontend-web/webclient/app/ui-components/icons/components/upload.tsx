import * as React from "react";

const SvgUpload = props => (
  <svg
    viewBox="0 0 24 24"
    fillRule="evenodd"
    clipRule="evenodd"
    fill="currentcolor"
    {...props}
  >
    <path
      d="M5.098 13.761l.012 2.753c-1.914-.006-3.739-1.262-4.582-3.047-.673-1.425-.702-3.147-.094-4.608.481-1.153 1.339-2.134 2.417-2.724 0 0 .086-.327.176-.582C3.57 4.033 4.92 2.83 6.48 2.597c.399-.059.805-.057 1.203.003C9.097.978 11.151.02 13.27 0h.106c3.281.03 6.384 2.381 7.276 5.772.03.114.068.278.068.278 2.114.868 3.52 3.302 3.246 5.663-.296 2.559-2.47 4.773-5.052 4.801l-.02-2.753c1.355-.014 2.53-1.412 2.436-2.864-.075-1.133-.906-2.197-2.005-2.41L18.33 8.3l-.113-1.054c-.27-2.378-2.303-4.375-4.649-4.49a4.736 4.736 0 0 0-.177-.004l-.08-.001c-1.563.01-3.095.827-4.007 2.16l-.632.935c-.724-.303-1.454-.728-2.162-.425-.628.268-1.066.95-1.087 1.664l-.018 1.01c-.675.223-1.37.389-1.896.915-.686.685-1.008 1.746-.771 2.714.27 1.102 1.241 2.024 2.36 2.036z"
      fill={props.color2 ? props.color2 : null}
    />
    <path
      d="M11.999 9.999l-6 7.5h12l-6-7.5zM1.002 21h22v3h-22z"
      fill={undefined}
    />
  </svg>
);

export default SvgUpload;
