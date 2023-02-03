import * as React from "react";
import {DBTransactionEvent, DebugContext, DebugContextType, getEvent, Log, MessageImportance} from "../WebSockets/Schema";
import {activeService, DebugContextAndChildren, isLog, logStore, replayMessages} from "../WebSockets/Socket";
import "./MainContent.css";
import {FixedSizeList as List} from "react-window";
import AutoSizer from "react-virtualized-auto-sizer";

// Notes/Issues:
//  Fetching missing contexts sometimes misses some. Backend issue. 
//  Clicking the same ctx several times doesn't always respond with every log. Seems to find ctxes.
//  Frontend styling is generally not good.
//  Handle different types of ctx/logs to render.
//  Blob searching support is missing!
//  Height of list is too high when active context/log.
//  What happens when selecting a different service?
//     - Works, but what other behavior should we expect? Maybe clear a service contexts when more than 5 minutes since activation (and not selected).
//  Handle long-running situations where memory usage has become high.
//  seekToEnd sometimes crashes.

type LogOrCtx = Log | DebugContext;
const ITEM_SIZE = 22;

export function MainContent(): JSX.Element {
    const [routeComponents, setRouteComponents] = React.useState<LogOrCtx[]>([]);
    const service = React.useSyncExternalStore(s => activeService.subscribe(s), () => activeService.getSnapshot());
    const logs = React.useSyncExternalStore(s => logStore.subscribe(s), () => logStore.getSnapshot())

    const setContext = React.useCallback((d: DebugContext | null) => {
        if (d === null) {
            if (logStore.contextRoot() != null) {
                console.log("clearActiveContext")
                logStore.clearActiveContext();
            }
            setRouteComponents([]);
            return;
        }
        logStore.addDebugRoot(d);
        replayMessages(activeService.generation, d.id, d.timestamp);
        setRouteComponents([d]);
    }, [setRouteComponents]);

    const serviceLogs = logs.content[service] ?? [];
    const activeContext = routeComponents.at(-1);

    return <div className="main-content">
        {!service ? <h3>Select a service to view requests</h3> :
            <>
                <BreadCrumbs clearContext={() => setContext(null)} routeComponents={routeComponents} setRouteComponents={setRouteComponents} />
                <RequestDetails key={activeContext?.id} activeContext={activeContext} />
                <AutoSizer defaultHeight={200}>
                    {({height, width}) => {
                        const root = logStore.contextRoot();
                        if (root) {
                            return <List itemSize={ITEM_SIZE} height={height} width={width} itemCount={1} itemData={root} key={logStore.entryCount} className="card">
                                {({data: root, style}) =>
                                    <DebugContextRow
                                        style={style}
                                        setRouteComponents={ctx => setRouteComponents(ctx)}
                                        debugContext={root.ctx}
                                        ctxChildren={root.children}
                                        isActive={false}
                                    />
                                }
                            </List>
                        }
                        return <List itemData={serviceLogs} height={height} width={width} itemSize={ITEM_SIZE} itemCount={serviceLogs.length} className="card" data-activecontext={activeContext == null}>
                            {({index, data, isScrolling, style}) => {
                                const item = data[index];
                                console.log(isScrolling);
                                return <DebugContextRow
                                    key={item.id}
                                    style={style}
                                    setRouteComponents={() => {setContext(item); setRouteComponents([item]);}}
                                    debugContext={item}
                                    isActive={activeContext === item}
                                />
                            }}
                        </List>;
                    }}
                </AutoSizer>
            </>
        }
    </div>
}

function DebugContextRow({debugContext, setRouteComponents, isActive, ctxChildren, style}: {
    isActive: boolean;
    debugContext: DebugContext;
    setRouteComponents(ctx: LogOrCtx[]): void;
    ctxChildren?: (DebugContextAndChildren | Log)[];
    style?: React.CSSProperties | undefined;
}): JSX.Element {
    const children = ctxChildren ?? [];
    return <>
        <div
            key={debugContext.id}
            className="request-list-row flex"
            onClick={() => setRouteComponents([debugContext])}
            data-selected={isActive}
            style={style}
            data-haschildren={children.length > 0}
            data-has-error={hasError(debugContext.importance)}
            data-is-odd={isOdd(debugContext.importance)}
        >
            <div>{debugContext.name}</div>
        </div>
        <div className="ml-24px">
            {children.map(it => {
                if (isLog(it)) {
                    return <div key={it.id} onClick={() => setRouteComponents([debugContext, it])} className="flex request-list-row left-border-black">{it.message.previewOrContent}</div>
                } else {
                    return <DebugContextRow
                        key={it.ctx.id}
                        setRouteComponents={ctx => setRouteComponents([debugContext, ...ctx])}
                        style={{borderLeft: "solid 1px black"}}
                        debugContext={it.ctx}
                        isActive={false}
                        ctxChildren={it.children}
                    />
                }
            })}
        </div>
    </>
}

function hasError(importance: MessageImportance): boolean {
    return [MessageImportance.THIS_IS_WRONG, MessageImportance.THIS_IS_DANGEROUS].includes(importance);
}

function isOdd(importance: MessageImportance): boolean {
    return importance === MessageImportance.THIS_IS_ODD;
}

function RequestDetails({activeContext}: {activeContext: LogOrCtx | undefined}): JSX.Element {
    if (!activeContext) return <div />;
    return <div className="card details flex">
        <RequestDetailsByType activeContext={activeContext} />
    </div>;
}

function RequestDetailsByType({activeContext}: {activeContext: LogOrCtx}): JSX.Element {
    if (isLog(activeContext)) {
        return <div>{activeContext.message.previewOrContent}</div>
    }

    switch (activeContext.type) {
        case DebugContextType.DATABASE_TRANSACTION:
            const event = getEvent(activeContext);
            const asString = DBTransactionEvent[event];
            return <>
                <div className="card query">
                    DATABASE_TRANSACTION
                    {event}, {asString}
                    <pre>{activeContext.name}</pre>
                </div>
                <div className="card query-details">
                    <pre>
                        {activeContext.id}
                    </pre>
                </div>
            </>;
        case DebugContextType.SERVER_REQUEST:
            return <>
                <div className="card query">
                    SERVER_REQUEST
                    <pre>{activeContext.name}</pre>
                </div>
                <div className="card query-details">
                    <pre>
                        {activeContext.id}
                    </pre>
                </div>
            </>;
        case DebugContextType.CLIENT_REQUEST:
            return <>
                <div className="card query">
                    CLIENT_REQUEST
                    <pre>{activeContext.name}</pre>
                </div>
                <div className="card query-details">
                    <pre>
                        {activeContext.id}
                    </pre>
                </div>
            </>;
        case DebugContextType.BACKGROUND_TASK:
            return <>
                <div className="card query">
                    BACKGROUND_TASK
                    <pre>{activeContext.name}</pre>
                </div>
                <div className="card query-details">
                    <pre>
                        {activeContext.id}
                    </pre>
                </div>
            </>;
        case DebugContextType.OTHER:
            return <>
                <div className="card query">
                    OTHER TODO
                    <pre>{activeContext.name}</pre>
                </div>
                <div className="card query-details">
                    <pre>
                        {activeContext.id}
                    </pre>
                </div>
            </>;
    }
}

function BreadCrumbs({routeComponents, setRouteComponents, clearContext}: {clearContext(): void; routeComponents: LogOrCtx[]; setRouteComponents: React.Dispatch<React.SetStateAction<LogOrCtx[]>>}): JSX.Element {

    const setToParentComponent = React.useCallback((id: number) => {
        if (id === -1) {
            setRouteComponents([]);
            clearContext();
        }
        setRouteComponents(r => r.slice(0, id + 1));
    }, [setRouteComponents, clearContext]);

    if (routeComponents.length === 0) return <div />
    return <div className="flex full-width">
        <div className="breadcrumb pointer" onClick={() => setToParentComponent(-1)}>Root</div>
        {routeComponents.map((it, idx) => <div key={it.id} className="breadcrumb pointer" onClick={() => setToParentComponent(idx)}>{prettierString(it.typeString)}</div>)}
    </div>;
}

export function prettierString(str: string): string {
    if (str.length === 0 || str.length === 1) return str;
    const lowerCasedAndReplaced = str.toLocaleLowerCase().replaceAll("_", " ");
    return lowerCasedAndReplaced[0].toLocaleUpperCase() + lowerCasedAndReplaced.slice(1);
}