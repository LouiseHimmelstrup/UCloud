import styled from "styled-components";
import * as React from "react";
import {Button} from "ui-components/index";
import {useCallback, useLayoutEffect, useRef, useState} from "react";
import {ButtonProps} from "ui-components/Button";
import Icon, {IconName} from "ui-components/Icon";
import {shakeAnimation} from "UtilityComponents";
import {doNothing} from "UtilityFunctions";
import {fontSize, FontSizeProps} from "styled-system";
import {selectHoverColor, ThemeColor} from "ui-components/theme";

const Wrapper = styled(Button)<{ align?: "left" | "center", hoverColor?: string } & FontSizeProps>`
  --progress-border: var(--background, #f00);
  --progress-active: var(--white, #f00);
  --progress-success: var(--color, #f00);
  --color: var(--${p => p.textColor}, #f00);
  --background: var(--${p => p.color}, #f00);
  --tick-stroke: var(--progress-active);

  ${shakeAnimation};
  ${fontSize};

  outline: none;
  user-select: none;
  cursor: pointer;
  backface-visibility: hidden;
  -webkit-appearance: none;
  -webkit-tap-highlight-color: transparent;
  min-width: ${p => p.asSquare ? "200px" : "250px"};
  background: var(--background, #f00);
  font-size: ${p => p.asSquare ? "16px" : "large"};
  font-weight: ${p => p.asSquare ? "400" : "700"};
  
  &:hover {
    ${p => p.asSquare ? ({
        "--progress-border": `var(--${p.hoverColor ?? selectHoverColor(p.color ?? "blue")}, #f00)`,
        "--background": `var(--${p.hoverColor ?? selectHoverColor(p.color ?? "blue")}, #f00)`
    }) : ({})}
  }

  & > .icons {
    border-radius: 50%;
    top: 9px;
    left: 15px;
    position: absolute;
    background: var(--progress-border);
    transition: transform .3s, opacity .2s;
    opacity: var(--icon-o, 0);
    transform: translateX(var(--icon-x, -4px));
  }

  & > .icons:before {
    content: '';
    width: 16px;
    height: 16px;
    left: 2px;
    top: 2px;
    z-index: 1;
    position: absolute;
    background: var(--background);
    border-radius: inherit;
    transform: scale(var(--background-scale, 1));
    transition: transform .32s ease;
  }

  .icons > svg {
    display: block;
    fill: none;
    width: 20px;
    height: 20px;
  }

  .icons > svg.progress {
    transform: rotate(-90deg) scale(var(--progress-scale, 1));
    transition: transform .5s ease;
  }

  .icons > svg.progress circle {
    stroke-dashoffset: 1;
    stroke-dasharray: var(--progress-array, 0) 52;
    stroke-width: 16;
    stroke: var(--progress-active);
    transition: stroke-dasharray var(--duration) linear;
  }

  .icons > svg.tick {
    left: 0;
    top: 0;
    position: absolute;
    stroke-width: 3;
    stroke-linecap: round;
    stroke-linejoin: round;
    stroke: var(--tick-stroke);
    transition: stroke .3s ease .7s;
  }

  .icons > svg.tick polyline {
    stroke-dasharray: 18 18 18;
    stroke-dashoffset: var(--tick-offset, 18);
    transition: stroke-dashoffset .4s ease .7s;
  }

  ul {
    padding: 0;
    margin: 0;
    ${p => p.align !== "left" ? ({
      textAlign: "center",
    }) : ({
      textAlign: "left",
    })}
    ${p => p.align === "left" && p.asSquare ? ({ marginLeft: "34px"}) : ({})}
    pointer-events: none;
    list-style: none;
    min-width: 80%;
    backface-visibility: hidden;
    transition: transform .3s;
    position: relative;
  }

  ul li {
    backface-visibility: hidden;
    transform: translateY(var(--ul-y)) translateZ(0);
    transition: transform .3s ease .16s, opacity .2s ease .16s;
  }

  ul li:not(:first-child) {
    --o: 0;
    position: absolute;
    left: 0;
    right: 0;
  }

  ul li:nth-child(1) {
    opacity: var(--ul-o-1, 1);
  }

  ul li:nth-child(2) {
    top: 100%;
    opacity: var(--ul-o-2, 0);
  }

  ul li:nth-child(3) {
    top: 200%;
    opacity: var(--ul-o-3, 0);
  }

  &.process {
    --icon-x: 0;
    --ul-y: -100%;
    --ul-o-1: 0;
    --ul-o-2: 1;
    --ul-o-3: 0;
  }

  &.process,
  &.success {
    --icon-o: 1;
    --progress-array: 52;
  }

  &.process > .ucloud-native-icons, &.success > .ucloud-native-icons {
    opacity: 0;
  }

  .ucloud-native-icons {
    position: absolute;
    left: 15px;
  }

  & {
    transform: scale(1);
  }

  &:hover {
    ${p => !p.asSquare ? ({
      transform: "translateY(-2px)"
    }) : ({
      transform: "scale(1)"
    })}
  }

  &.success {
    --icon-x: 6px;
    --progress-border: none;
    --progress-scale: .11;
    --tick-stroke: var(--progress-success);
    --background-scale: 0;
    --tick-offset: 36;
    --ul-y: -200%;
    --ul-o-1: 0;
    --ul-o-2: 0;
    --ul-o-3: 1;
  }

  &.success > .icons svg.progress {
    animation: tick .3s linear forwards .4s;
  }

  @keyframes tick {
    100% {
      transform: rotate(-90deg) translate(0, -5px) scale(var(--progress-scale));
    }
  }
`;

export const ConfirmationButton: React.FunctionComponent<ButtonProps & {
    actionText: string,
    doneText?: string,
    icon: IconName,
    align?: "left" | "center",
    onAction?: () => void;
    hoverColor?: ThemeColor;
}> = props => {
    const buttonRef = useRef<HTMLButtonElement>(null);
    const timeout = useRef(-1);
    const timer = useRef(1600);
    const tickRate = 50;
    const [showHelp, setShowHelp] = useState(false);
    const wasReset = useRef(false);

    const success = useCallback(() => {
        const button = buttonRef.current;
        if (!button) return;
        timer.current -= tickRate;
        if (timer.current <= 0) {
            button.classList.add("success");
            timeout.current = window.setTimeout(countUp, tickRate);
            setTimeout(() => {
                if (props.onAction) props.onAction();
            }, 1000);
        } else {
            timeout.current = window.setTimeout(success, tickRate);
        }
    }, [buttonRef.current, props.onAction]);

    const countUp = useCallback(() => {
        const button = buttonRef.current;
        if (!button) return;
        timer.current += tickRate;
        if (timer.current >= 1600) {
            timer.current = 1600;
        } else {
            timeout.current = window.setTimeout(countUp, tickRate);
        }
    }, [buttonRef.current]);

    const start = useCallback(() => {
        const button = buttonRef.current;
        if (!button) return;
        if (button.classList.contains("process")) return;
        if (timeout.current !== -1) {
            clearTimeout(timeout.current);
            timeout.current = -1;
        }

        if (button.classList.contains("success")) {
            wasReset.current = true;
        }

        button.classList.remove("success");
        button.classList.add("process");
        timeout.current = window.setTimeout(success, tickRate);
    }, [buttonRef.current, success]);

    const end = useCallback(() => {
        const button = buttonRef.current;
        if (!button) return;
        button.classList.remove("process");
        if (timeout.current !== -1) {
            clearTimeout(timeout.current);
            timeout.current = window.setTimeout(countUp, tickRate);
        }

        if (timer.current > 1500 && !wasReset.current) {
            button.classList.add("shaking");
            setShowHelp(true);
            setTimeout(() => {
                setShowHelp(false);
                buttonRef.current?.classList?.remove("shaking");
            }, 1500);
        }

        wasReset.current = false;
    }, [buttonRef.current, timeout]);

    useLayoutEffect(() => {
        const button = buttonRef.current;
        if (!button) return;

        button.style.setProperty("--duration", "1600ms");
    }, [buttonRef.current]);

    const passedProps = {...props};
    delete passedProps.onAction;

    return <Wrapper {...passedProps} onMouseDown={start} onTouchStart={start} onMouseUp={end} onTouchEnd={end}
                    onClick={doNothing} ref={buttonRef}>
        <div className={"ucloud-native-icons"}>
            <Icon name={props.icon} size={"20"} mb="3px" />
        </div>
        <div className={"icons"}>
            <svg className="progress" viewBox="0 0 32 32">
                <circle r="8" cx="16" cy="16"/>
            </svg>
            <svg className="tick" viewBox="0 0 24 24">
                <polyline points="18,7 11,16 6,12"/>
            </svg>
        </div>
        <ul>
            <li>{showHelp ? "Hold to confirm" : props.actionText}</li>
            <li>Hold to confirm</li>
            <li>{props.doneText ?? "Done"}</li>
        </ul>
    </Wrapper>;
};
