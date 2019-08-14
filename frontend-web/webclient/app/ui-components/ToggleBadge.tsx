import styled from 'styled-components'
import theme, { ThemeColor } from './theme'
import { SpaceProps, FontSizeProps, ColorProps, space, fontSize, BackgroundColorProps } from 'styled-system';

const ToggleBadge = styled.button<ToggleBadge>`
  border-radius: ${props => props.theme.radius};
  border: 0;
  display: inline-block;
  font-weight: ${props => props.theme.bold};
  font-family: inherit;
  cursor: pointer;
  background-color: ${props =>
    props.selected ? props.theme.colors[props.bg] : props.unSelectedBg};
  color: ${(props: any) => props.theme.colors[props.color]};
  ${space};
  ${fontSize};
  &:hover {
    background-color: ${(props) => props.theme.colors[props.bg]};
  }
`;

ToggleBadge.displayName = "ToggleBadge";

interface ToggleBadge extends SpaceProps, FontSizeProps, ColorProps, BackgroundColorProps {
  selected?: boolean
  unSelectedBg?: string
  bg: ThemeColor
}

ToggleBadge.defaultProps = {
  selected: false,
  fontSize: 0,
  theme: theme,
  color: 'blue',
  bg: 'lightBlue',
  unSelectedBg: 'transparent'
};

export default ToggleBadge
