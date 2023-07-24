import * as React from "react";
import {ButtonStyleProps, HeightProps, SizeProps, SpaceProps, WidthProps} from "styled-system";
import {ThemeColor} from "./theme";
import {extractEventHandlers, extractSize, injectStyle, unbox, WithEventHandlers} from "@/Unstyled";

// TODO(Dan): A lot of these are left in to not break existing code, many of them are not actually supposed
//  to do anything anymore.
export interface ButtonProps extends ButtonStyleProps, HeightProps, SpaceProps, SizeProps, WidthProps, WithEventHandlers {
    fullWidth?: boolean;
    textColor?: ThemeColor;
    color?: ThemeColor;
    backgroundColor?: ThemeColor;
    lineHeight?: number | string;
    title?: string;
    attached?: boolean;
    asSquare?: boolean;
    children?: React.ReactNode;
    disabled?: boolean;
    type?: string;
    disableStandardSizes?: boolean;
    standardSize?: StandardButtonSize;
    btnRef?: React.RefObject<HTMLButtonElement>;
    borderRadius?: string;
    className?: string;
}

export enum StandardButtonSize {
    LARGE = 0,
    STANDARD = 1,
    SMALL = 2,
    EXTRA_SMALL = 3,
}

export const ButtonClass = injectStyle("button", k => `
    ${k} {
        display: inline-flex;
        justify-content: center;
        text-align: center;
        text-decoration: none;
        font-family: inherit;
        font-weight: normal;
        cursor: pointer;
        background-color: var(--blue, #f00);
        color: var(--white, #f00);
        border-width: 0;
        border-style: solid;
        display: flex;
        align-items: center;
    }

    ${k}:hover {
        filter: brightness(120%);
    }
    
    ${k}:disabled {
        opacity: 0.25;
    }

    ${k}:focus {
        outline: none;
    }

    ${k}[data-attached=true] {
        border-top-left-radius: 0;
        border-bottom-left-radius: 0;   
    }
    
    ${k}[data-square=true] {
        border-radius: 0 !important;
    }
    
    ${k}[data-fullwidth=true] {
        width: 100%;
    }
    
    ${k}[data-size=large] {
        height: 55px;
        padding: 15px 45px;
        border-radius: 15px;
        font-size: 19px;
    }
    
    ${k}, ${k}[data-size=standard] {
        height: 45px;
        padding: 12px 36px;
        border-radius: 8px;
        font-size: 15px;
    }
    
    ${k}[data-size=small] {
        height: 35px;
        padding: 8px 24px;
        border-radius: 8px;
        font-size: 15px;
    }
    
    ${k}[data-size=extra-small] {
        height: 25px;
        padding: 6px 18px;
        border-radius: 14px;
        font-size: 14px;
    }
`);

const standardButtonSizes: {height: number; name: string;}[] = [
    {height: 55, name: "large"},
    {height: 45, name: "standard"},
    {height: 35, name: "small"},
    {height: 25, name: "extra-small"},
];

export const Button: React.FunctionComponent<ButtonProps> = props => {
    const inlineStyles = unbox(props);
    let sizeName: string | undefined = undefined;
    //inlineStyles.backgroundColor = `var(--${props.color ?? "blue"})`;
    inlineStyles.color = `var(--${props.textColor ?? "white"})`;
    if (props.disableStandardSizes !== true) {
        let bestMatch = 1;
        if (props.standardSize !== undefined || props.height === undefined) {
            bestMatch = props.standardSize ?? StandardButtonSize.STANDARD;
        } else {
            let height = parseInt(extractSize(props.height));
            if (isNaN(height)) height = 45;

            let diff = 1000000000000000000;
            for (let i = 0; i < standardButtonSizes.length; i++) {
                const newDiff = Math.abs(height - standardButtonSizes[i].height);
                if (newDiff < diff) {
                    bestMatch = i;
                    diff = newDiff;
                }
            }
        }

        const match = standardButtonSizes[bestMatch];
        sizeName = match.name;
        delete inlineStyles["padding"];
        delete inlineStyles["paddingLeft"];
        delete inlineStyles["paddingTop"];
        delete inlineStyles["paddingRight"];
        delete inlineStyles["paddingLeft"];
        delete inlineStyles["paddingHeight"];
        delete inlineStyles["fontSize"];
    }

    return <button
        className={ButtonClass + " " + (props.className ?? "")}
        data-attached={props.attached === true}
        data-square={props.asSquare === true}
        data-fullwidth={props.fullWidth === true}
        data-size={sizeName}
        style={inlineStyles}
        disabled={props.disabled}
        type={props.type as any}
        {...extractEventHandlers(props)}
        ref={props.btnRef}
    >
        {props.children}
    </button>
}

Button.displayName = "Button";

export default Button;
