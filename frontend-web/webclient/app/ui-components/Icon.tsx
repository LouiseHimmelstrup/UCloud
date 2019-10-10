import * as CSS from "csstype";
import * as React from "react";
import styled from "styled-components";
import {color, ResponsiveValue, space, SpaceProps, style} from "styled-system";
import Bug from "./Bug";
import * as icons from "./icons";
import theme, {Theme} from "./theme";
import {Cursor} from "./Types";


const IconBase = ({name, size, theme, color2, spin, hoverColor, ...props}: IconBaseProps): JSX.Element => {
  const key = 0;
  let Component = icons[name];
  if (!Component) {
    if (name === "bug") {
      Component = Bug;
    } else {
      return (<></>);
    }
  }
  return (
    <Component
      key={key.toString()}
      width={size}
      height={size}
      color2={color2 ? theme.colors[color2] : undefined}
      {...props}
    />
  );
};

const hoverColor = style({
  prop: "hoverColor",
  cssProperty: "color",
  key: "colors",
});

export interface IconBaseProps extends SpaceProps, React.SVGAttributes<HTMLDivElement> {
  name: IconName | "bug";
  color?: string;
  color2?: string;
  rotation?: number;
  theme: Theme;
  cursor?: Cursor;
  size?: string | number;
  spin?: boolean;
  hoverColor?: ResponsiveValue<CSS.ColorProperty>;
  title?: string;
}

const spin = (props: {spin?: boolean}) => props.spin ? `
  animation: spin 1s linear infinite;
  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }
` : null;

const Icon = styled(IconBase) <IconBaseProps>`
  flex: none;
  vertical-align: middle;
  cursor: ${props => props.cursor};
  ${props => props.rotation ? `transform: rotate(${props.rotation}deg);` : ""}
  ${space} ${color};
  ${spin};

  &:hover {
    ${hoverColor};
  }

`;

Icon.displayName = "Icon";

Icon.defaultProps = {
  theme,
  cursor: "inherit",
  name: "notification",
  size: 24
};

// Use to see every available icon in debugging.
export const EveryIcon = () => (
  <>
    {Object.keys(icons).map((it: IconName, i: number) =>
      (<span key={i}><span>{it}</span>: <Icon name={it} key={i} />, </span>)
    )}
  </>
);

export type IconName = keyof typeof icons;

export default Icon;
