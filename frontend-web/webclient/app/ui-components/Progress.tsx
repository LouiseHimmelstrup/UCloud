import * as React from "react";
import Flex from "./Flex";
import Text from "./Text";
import {ThemeColor} from "./theme";
import {injectStyle} from "@/Unstyled";
import {CSSProperties} from "react";

const ProgressBaseClass = injectStyle("progress-base", k => `
    ${k} {
        border-radius: 5px;
        background-color: var(--progressColor, #f00);
        width: 100%;
        
        --progressColor: var(--successMain):
    }
    
    ${k}[data-active="false"] {
        display: none;
    }
    
    ${k}[data-pulse="true"] {
        height: 100%;
        
        /* From semantic-ui-css */
        animation: progress-active 2s ease infinite;
        color: black;
        width: 100%;
    }
    
    @keyframes progress-active {
        0% {
            opacity: 0.3;
            width: 0;
        }
        100% {
            opacity: 0;
            width: 100%;
        }
    }
`);

interface Progress {
    color: ThemeColor;
    percent: number;
    active: boolean;
    label: string;
}

const Progress = ({color, percent, active, label}: Progress): JSX.Element => {
    const topLevelStyle: CSSProperties = {height: "30px"};
    topLevelStyle["--progressColor"] = `var(--${color})`;

    const secondaryStyle = {...topLevelStyle};
    secondaryStyle.width = `${percent}%`;

    return (
        <>
            <div className={ProgressBaseClass} style={topLevelStyle}>
                <div className={ProgressBaseClass} style={secondaryStyle}>
                    <div className={ProgressBaseClass} data-pulse={"true"} data-active={active} />
                </div>
            </div>
            {label ? <Flex justifyContent="center"><Text>{label}</Text></Flex> : null}
        </>
    );
};

const NewAndImprovedProgressStyle = injectStyle("progress", k => `
    ${k} {
        margin-top: 15px;
        height: 5px;
        width: 250px;
        border-radius: 5px;
        position: relative;
        display: inline-flex;
        background: linear-gradient(
        120deg,
        #4BD823 0%, #389F1A var(--percentage),
        #ECEEF0 var(--percentage), #D0D5DC  var(--limit)
        );
    }
    
    ${k}:before {
        content: '';
        border-radius: 0 5px 5px 0;
        position: absolute;
        align-content: center;
        top: 0;
        right: 0;
        width: calc(100% - var(--limit));
        height: 100%;
        background: repeating-linear-gradient(
        135deg,
        #E11005 0 3px,
        #FF805F 3px 6px
        );
    }
    
    ${k}:after {
        content: attr(data-label);
        font-size: 12px;
        position: absolute;
        color: black;
        text-align: center;
        align-content: center;
        top: -1.4em;
        width: 100%;
    }
`)

const DEBUGGING_PURPOSES = DEVELOPMENT_ENV;
export function NewAndImprovedProgress({label, percentage, limit}: {label: string; percentage: number; limit: number;}) {
    React.useEffect(() => {
        if (DEBUGGING_PURPOSES) {
            if (percentage > 100) {
                console.warn("Percentage for", label, "is above 100")
            }

            if (limit > 100) {
                console.warn("limit for", label, "is above 100")
            }
        }
    }, []);

    const style: CSSProperties = {};
    style["--percentage"] = percentage + "%";
    style["--limit"] = limit + "%";
    return <div className={NewAndImprovedProgressStyle} data-label={label} style={style} />
}

export default Progress;
