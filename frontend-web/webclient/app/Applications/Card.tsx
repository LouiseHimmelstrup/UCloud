import {AppToolLogo} from "@/Applications/AppToolLogo";
import * as React from "react";
import styled, {css} from "styled-components";
import {Absolute, Flex, Icon, Relative, Truncate} from "@/ui-components";
import Box from "@/ui-components/Box";
import Link from "@/ui-components/Link";
import Markdown from "@/ui-components/Markdown";
import {EllipsedText, TextClass} from "@/ui-components/Text";
import theme from "@/ui-components/theme";
import * as Pages from "./Pages";
import {compute} from "@/UCloud";
import ApplicationWithFavoriteAndTags = compute.ApplicationWithFavoriteAndTags;
import {injectStyle, injectStyleSimple} from "@/Unstyled";
import {stopPropagationAndPreventDefault} from "@/UtilityFunctions";
import BaseLink, {BaseLinkClass} from "@/ui-components/BaseLink";

interface ApplicationCardProps {
    onFavorite?: (name: string, version: string) => void;
    app: Pick<ApplicationWithFavoriteAndTags, "tags" | "metadata">;
    isFavorite?: boolean;
    linkToRun?: boolean;
    colorBySpecificTag?: string;
    tags: string[];
}

const AppCardBase = injectStyle("app-card-base", k => `
    ${k} {
        padding: 10px;
        width: 100%;
        display: flex;
        align-items: center;
    }

    ${k} > img {
        width: 32px;
        height: 32px;
        margin-right: 16px;
        border-radius: 5px;
        flex-shrink: 0;
    }

    ${k} > strong {
        margin-right: 16px;
        font-weight: bold;
        flex-shrink: 0;
    }

    ${k} > .${TextClass} {
        color: var(--gray, #f00);
        flex-grow: 1;
    }

    ${k} > .${TextClass} > p {
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
    }
`);

export const ApplicationCardContainer = injectStyle("application-card-container", k => `
    ${k} {
        display: flex;
        flex-direction: column;
    }

    ${k} > ${AppCardBase}:first-child {
        border: 1px solid var(--borderGray, #f00);
        border-top-left-radius: 5px;
        border-top-right-radius: 5px;
    }

    ${k} > ${AppCardBase} {
        border: 1px solid var(--borderGray, #f00);
        border-top: 0;
    }

    ${k} > ${AppCardBase}:last-child {
        border-bottom-left-radius: 5px;
        border-bottom-right-radius: 5px;
    }
`);

export const SlimApplicationCard: React.FunctionComponent<ApplicationCardProps> = (props) => {
    const {metadata} = props.app;
    return (
        <Link className={AppCardBase} to={Pages.runApplication(metadata)}>
            <Box mr={16}>
                <AppToolLogo name={metadata.name} type={"APPLICATION"} size={"32px"} />
            </Box>
            <b>{metadata.title}</b>
            <EllipsedText>
                <Markdown
                    disallowedElements={[
                        "break",
                        "paragraph",
                        "emphasis",
                        "strong",
                        "thematicBreak",
                        "blockquote",
                        "delete",
                        "link",
                        "image",
                        "linkReference",
                        "imageReference",
                        "table",
                        "tableRow",
                        "tableCell",
                        "list",
                        "listItem",
                        "definition",
                        "heading",
                        "inlineCode",
                        "code",
                        "html"]}
                    unwrapDisallowed
                >
                    {metadata.description}
                </Markdown>
            </EllipsedText>
            <Flex><Icon name="chevronDown" size={"18px"} rotation={-90} /></Flex>
        </Link>
    );
};

export const Tag = ({label, bg = "darkGray"}: {label: string; bg?: string}): JSX.Element => (
    <div style={{
        marginRight: "3px",
        background: `var(--${bg})`,
        display: "inline-block",
        lineHeight: 1.5,
        color: "white",
        textTransform: "uppercase",
        fontSize: "10px",
        fontWeight: 700,
        borderRadius: "6px",
        padding: "3px"
    }}>
        {label}
    </div>
);

const appColors = theme.appColors;

const nColors = appColors.length;

const bgGradients = appColors.map(x => (`linear-gradient(0deg, ${x[0]}, ${x[2]})`));

interface AppLogoRawProps {
    color1Offset: number;
    color2Offset: number;
    appC: number;
    rot: number;
    size: string;
}


const ROT120 = "rotate(120 0 0)";
const ROT240 = "rotate(240 0 0)";
const S32 = Math.sqrt(3) * .5;
const R1 = 0.5; // inner radius of outer element (outer radius is 1)
const R2 = 0.7; // outer radius of inner element
const R3 = (1 + R2) * .5; // radius of white background hexagon
const CENTER_C = nColors - 1;
export const AppLogoRaw = ({rot, color1Offset, color2Offset, appC, size}: AppLogoRawProps): JSX.Element => {
    const c1 = [color1Offset % 3, (color1Offset + 1) % 3, (color1Offset + 2) % 3];
    const c2 = [color2Offset % 3, (color2Offset + 1) % 3, (color2Offset + 2) % 3];

    return (
        <svg
            width={size}
            height={size}
            viewBox={`-1 -${S32} 2 ${2 * S32}`}
            fillRule="evenodd"
            clipRule="evenodd"
        >
            <defs>
                <path id="hex_to___" d={`M-${R1} 0H-1L-0.5 ${S32}H0.5L${(0.5 * R1)} ${S32 * R1}H-${0.5 * R1}Z`} />
                <path id="hex_ti___" d={`M0 0H${R2}L${0.5 * R2} -${S32 * R2}H-${0.5 * R2}Z`} fillOpacity=".55" />
                <path
                    id="hex_th___"
                    d={`M-${R3} 0L-${0.5 * R3} ${S32 * R3}H${0.5 * R3}L${R3} 0L${0.5 * R3} -${S32 * R3}H-${0.5 * R3}Z`}
                />
            </defs>
            <g transform={`rotate(${rot} 0 0)`}>
                <use xlinkHref="#hex_th___" fill="#fff" />
                <use xlinkHref="#hex_to___" fill={appColors[appC][c1[0]]} />
                <use xlinkHref="#hex_to___" fill={appColors[appC][c1[1]]} transform={ROT120} />
                <use xlinkHref="#hex_to___" fill={appColors[appC][c1[2]]} transform={ROT240} />
                <use xlinkHref="#hex_ti___" fill={appColors[CENTER_C][c2[0]]} />
                <use xlinkHref="#hex_ti___" fill={appColors[CENTER_C][c2[1]]} transform={ROT120} />
                <use xlinkHref="#hex_ti___" fill={appColors[CENTER_C][c2[2]]} transform={ROT240} />
            </g>
        </svg>
    );
};

export const AppLogo = ({size, hash}: {size: string, hash: number}): JSX.Element => {
    const i1 = (hash >>> 30) & 3;
    const i2 = (hash >>> 20) & 3;
    const rot = [0, 15, 30];
    const i3 = (hash >>> 10) % rot.length;
    const appC = appColor(hash);

    return <AppLogoRaw rot={rot[i3]} color1Offset={i1} color2Offset={i2} appC={appC} size={size} />;
};

export function hashF(str: string): number {
    let hash = 5381;
    let i = str.length;

    while (i) {
        hash = (hash * 33) ^ str.charCodeAt(--i);
    }

    /* JavaScript does bitwise operations (like XOR, above) on 32-bit signed
     * integers. Since we want the results to be always positive, convert the
     * signed int to an unsigned by doing an unsigned bitshift. */

    return hash >>> 0;

}

export function appColor(hash: number): number {
    return (hash >>> 22) % (nColors - 1); // last color not used
}

export function CardToolContainer({children}: React.PropsWithChildren): JSX.Element {
    return <div className={CardToolContainerClass} children={children} />
}

const CardToolContainerClass = injectStyleSimple("card-tool-container", `
    display: grid;
    flex-direction: column;
    align-items: flex-start;
    border-radius: 5px;
    overflow: hidden;
    width: 100%;
`);

export const SmallCard = styled(Link) <{color1: string; color2: string; color3: string}>`
    display: flex;
    padding: 10px;
    width: 150px;
    height: 50px;
    border-radius: 10px;
    font-size: ${theme.fontSizes[2]}px;
    text-align: center;
    align-items: center;
    justify-content: center;
    background-color: ${p => p.color2};
    box-shadow: ${theme.shadows.sm};

    transition: transform ${theme.timingFunctions.easeIn} ${theme.duration.fastest} ${theme.transitionDelays.xsmall};

    &:hover {
        transition: transform ${theme.timingFunctions.easeOut} ${theme.duration.fastest} ${theme.transitionDelays.xsmall};
        transform: translateY(-2px);
        color: var(--white, #f00);
    }
`;

const MultiLineTruncateClass = injectStyleSimple("multiline-truncate", `
    display: -webkit-box;
    -webkit-box-orient: vertical;
    overflow: hidden;
    text-align: left;
    font-size: var(--secondaryText);
`);

const TallApplicationCard = injectStyle("tall-application-card", k => `
    ${k} {
        width: 156px;
        height: 240px;
        cursor: pointer;
    }

    ${k} > div.image {
        width: 75px;
        height: 75px;
        margin-top: 30px;
        margin-bottom: 8px;
    }
    
    ${k} > div.image > * {
        width: 52px;
        height: 52px;
    }

    ${k}:hover {
        /* TODO(Jonas): Missing inset box-shadow on hover. */
    }
`);

const TitleAndDescriptionClass = injectStyleSimple("title-and-description", "");

const WideApplicationCard = injectStyle("wide-application-card", k => `
    ${k} {
        display: flex;
        width: 322px;
        height: 142px;
    }

    ${k} > div.image {
        width: 84px;
        height: 84px;
        margin-top: auto;
        margin-bottom: auto;
        text-align: center;
    }

    ${k} > div.${TitleAndDescriptionClass} {
        margin-top: 8px;
        width: calc(322px - 100px);
    }

    ${k}[data-xl="true"] {
        width: 432px;
        height: 202px;
    }

    ${k}[data-xl="true"] > div.image {
        width: 100px;
        height: 100px;
    }

    ${k}[data-xl="true"] > div.${TitleAndDescriptionClass} {
        width: calc(432px - 140px);
    }
`);

const ApplicationCardClass = injectStyle("application-card", k => `
    ${k} {
        border-radius: 16px;
        box-shadow: ${theme.shadows.sm};
        background-color: var(--white);
        color: var(--text);
    }

    ${k} > div > span {
        text-align: left;
        font-size: var(--secondaryText);
        overflow-y: scroll;
        padding-left: 8px;
        padding-right: 8px;
        height: 90px;
    }
    
    ${k} > div.image > * {
        margin-left: auto;
        margin-right: auto;
        margin-top: auto;
        margin-bottom: auto;
    }

    ${k} > div > .${MultiLineTruncateClass} {
        margin-left: 10px;
        margin-right: 10px;
        color: var(--text);
    }

    ${k} > div.image {
        background-color: white;
        border-radius: 12px;
        margin-left: auto;
        margin-right: auto;
        display: flex;
    }

    ${k} > div {
        text-align: center;
    }
`);

function MultiLineTruncate(props: React.PropsWithChildren<{lines: number}>): JSX.Element {
    const {lines, ...p} = props;
    return <div className={MultiLineTruncateClass} style={{
        WebkitLineClamp: props.lines,
    }} {...p} />;
}

const FAV_ICON_SIZE = "24px";
const FavIcon = injectStyleSimple("app-fav-icon", `
    position: relative; 
    cursor: pointer;
    height: 0;
    width: 0;
    top: 4px;
    left: calc(100% - ${FAV_ICON_SIZE} - 4px);
`);

export enum ApplicationCardType {
    WIDE,
    EXTRA_TALL,
    TALL,
    EXTRA_WIDE,
}

export interface AppCardProps extends React.PropsWithChildren<ApplicationCardProps> {
    type: ApplicationCardType;
}

function lineCountFromType(t: ApplicationCardType): number {
    switch (t) {
        case ApplicationCardType.TALL:
            return 5;
        case ApplicationCardType.EXTRA_TALL:
            return 6;
        case ApplicationCardType.WIDE:
            return 5;
        case ApplicationCardType.EXTRA_WIDE:
            return 9;
    }
}

const FavoriteAppClass = injectStyle("favorite-app", k => `
    ${k} {
        width: 76px;
        height: 76px;
        border-radius: 99999px;
        box-shadow: rgba(0, 0, 0, 0.2) 0px 3px 5px -1px inset, rgba(0, 0, 0, 0.14) 0px 6px 10px 0px;
        display: flex;
        /* TODO(Jonas): This should be calculated much better. */
        padding-left: 14px;
        align-items: center;
    }
`);

export function FavoriteApp(props: {name: string, version: string, onFavorite(name: string, version: string): void;}): JSX.Element {
    return <>
        <Link to={Pages.run(props.name, props.version)}>
            <div className={FavoriteAppClass}>
                <AppToolLogo size="48px" name={props.name} type="APPLICATION" />
            </div>
        </Link>
        <Relative top="50px" right="24px" width="0px" height="0px">
            <Icon cursor="pointer" name="starFilled" color="blue" hoverColor="blue" size={FAV_ICON_SIZE} onClick={() => props.onFavorite(props.name, props.version)} />
        </Relative>
    </>
}

export function AppCard(props: AppCardProps): JSX.Element {

    const favorite = React.useCallback((e: React.SyntheticEvent) => {
        stopPropagationAndPreventDefault(e);
        props.onFavorite?.(props.app.metadata.name, props.app.metadata.version);
    }, [props.app.metadata]);


    return React.useMemo(() => {
        let lineCount = lineCountFromType(props.type);
        const app = props.app;
        const {metadata} = app;
        const favoriteDiv =
            <div className={FavIcon} onClick={favorite}>
                <Icon color="var(--blue)" hoverColor="blue" size={FAV_ICON_SIZE} name={"starEmpty"} />
            </div>
        const titleAndDescription =
            <div className={TitleAndDescriptionClass}>
                <div><b>{metadata.title}</b></div>
                <MultiLineTruncate lines={lineCount}>{metadata.description}</MultiLineTruncate>
            </div>;
        switch (props.type) {
            case ApplicationCardType.EXTRA_TALL:
            case ApplicationCardType.TALL:
                if (props.type === ApplicationCardType.EXTRA_TALL) console.log("TODO BY EXTRA_TALL");
                return <div className={ApplicationCardClass + " " + TallApplicationCard}>
                    {favoriteDiv}
                    <div className="image">
                        <AppToolLogo size={"52px"} name={app.metadata.name} type="APPLICATION" />
                    </div>
                    {titleAndDescription}
                </div>
            case ApplicationCardType.WIDE:
            case ApplicationCardType.EXTRA_WIDE:
                const isExtraWide = props.type === ApplicationCardType.EXTRA_WIDE;
                return <div className={ApplicationCardClass + " " + WideApplicationCard} data-xl={isExtraWide}>
                    {favoriteDiv}
                    <div className="image">
                        <AppToolLogo size={isExtraWide ? "85px" : "65px"} name={app.metadata.name} type="APPLICATION" />
                    </div>
                    {titleAndDescription}
                </div>
        }
    }, [props, favorite]);
}