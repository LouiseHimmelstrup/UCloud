import * as React from "react";

const SvgId = props => (
  <svg
    viewBox="0 0 24 24"
    fillRule="evenodd"
    clipRule="evenodd"
    fill="currentcolor"
    {...props}
  >
    <path
      d="M21 24.001H3a3 3 0 0 1-3-3v-18a3 3 0 0 1 3-3h18a3 3 0 0 1 3 3v18a3.004 3.004 0 0 1-3 3zm0-3v-18H3v18h18z"
      fill={props.color2 ? props.color2 : null}
    />
    <g fill="#3d4d65" fillRule="nonzero">
      <path d="M4.98 18.003v-1.88h1.869v-8.19h-1.87v-1.93h5.613v1.93h-1.87v8.19h1.87v1.88H4.979zM12.674 18.003v-12h2.337c1.075 0 1.874.228 2.396.684.524.457.917 1.146 1.179 2.068.261.922.393 2.017.393 3.28 0 1.195-.138 2.245-.416 3.15-.276.904-.67 1.6-1.177 2.087-.509.487-1.3.73-2.375.73h-2.337zm1.656-2.265h.686c.868 0 1.117-.077 1.488-.453.37-.376.477-.586.635-1.253.16-.667.192-1.04.192-2.215 0-1.148-.04-1.504-.224-2.124-.187-.617-.295-.81-.64-1.142-.349-.332-.578-.398-1.37-.398h-.767v7.585z" />
    </g>
  </svg>
);

export default SvgId;