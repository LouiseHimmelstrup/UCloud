import styled from "styled-components";
import {
    AlignItemsProps, color, ColorProps, height, HeightProps, maxHeight, MaxHeightProps, maxWidth, MaxWidthProps,
    minHeight, MinHeightProps, minWidth, MinWidthProps, overflow, OverflowProps, space, SpaceProps, textAlign,
    TextAlignProps, width, WidthProps, zIndex, ZIndexProps, background, BackgroundProps, flexGrow, flexShrink, 
    JustifyContentProps, justifyContent, borderRadius, BorderRadiusProps
} from "styled-system";
import {cursor} from "@/Utilities/StyledComponentsUtilities";
import {Cursor} from "./Types";

export type BoxProps =
    SpaceProps &
    WidthProps &
    MinWidthProps &
    ColorProps &
    BackgroundProps &
    AlignItemsProps &
    JustifyContentProps &
    HeightProps &
    MinHeightProps &
    MaxHeightProps &
    MaxWidthProps &
    FlexGrowProps &
    FlexShrinkProps &
    ZIndexProps &
    TextAlignProps &
    OverflowProps &
    BorderRadiusProps &
    {cursor?: Cursor};

interface FlexGrowProps {
    flexGrow?: number;
}

interface FlexShrinkProps {
    flexShrink?: number;
}

const Box = styled.div<BoxProps>`
  ${borderRadius}
  ${justifyContent}
  ${cursor}
  ${zIndex}
  ${flexGrow}
  ${flexShrink}
  ${space}
  ${width}
  ${minWidth}
  ${maxWidth}
  ${height}
  ${minHeight}
  ${maxHeight}
  ${color}
  ${textAlign}
  ${overflow}
  ${background}
`;

Box.displayName = "Box";

export default Box;
