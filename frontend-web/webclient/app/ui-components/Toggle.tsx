import * as React from "react";
import {injectStyle} from "@/Unstyled";
import {useCallback} from "react";

interface ToggleProps {
    checked: boolean;
    onChange: () => void;
}

export const Toggle: React.FC<ToggleProps> = ({
    checked,
    onChange
}) => {
    const handler = useCallback((e: React.SyntheticEvent) => {
        e.stopPropagation();
        e.preventDefault();
        onChange()
    }, [onChange]);

    return <div onClick={handler} data-active={checked} className={ToggleWrapperClass}>
        <div />
    </div>
}

const ToggleWrapperClass = injectStyle("toggle-wrapper", k => `
    ${k} {
        border-radius: 12px;
        height: 26px;
        width: 45px;
        background-color: var(--textSecondary);
        transition: 0.2s;
        padding-top: 2px;
        padding-left: 2px;
        cursor: pointer;
    }

    ${k}[data-active="true"] {
        background-color: var(--successMain);
        padding-left: 21px;
    }

    ${k} > div {
        border-radius: 50%;
        width: 22px;
        background-color: white;
        height: 22px;
    }
`);
