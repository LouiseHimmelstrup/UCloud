import * as React from "react";
import {useXTerm} from "Applications/Jobs/xterm";
import {WSFactory} from "Authentication/HttpClientInstance";
import {useEffect, useState} from "react";
import {useCloudAPI} from "Authentication/DataHook";
import {useParams} from "react-router";
import {Box, Button} from "ui-components";
import {isLightThemeStored, shortUUID, useEffectSkipMount, useNoFrame} from "UtilityFunctions";
import styled from "styled-components";
import {useTitle} from "Navigation/Redux/StatusActions";
import {compute} from "UCloud";
import jobs = compute.jobs;
import {TermAndShellWrapper} from "Applications/Jobs/TermAndShellWrapper";


export const Shell: React.FunctionComponent = () => {
    const {termRef, terminal, fitAddon} = useXTerm();
    const {jobId, rank} = useParams<{ jobId: string, rank: string }>();
    const [sessionResp, openSession] = useCloudAPI(
        jobs.openInteractiveSession({id: jobId, rank: parseInt(rank, 10), sessionType: "SHELL"}),
        {sessions: []},
    );

    const [closed, setClosed] = useState<boolean>(false);
    const [reconnect, setReconnect] = useState<number>(0);
    useNoFrame();
    useTitle(`Job ${shortUUID(jobId)} [Rank: ${parseInt(rank, 10) + 1}]`);

    useEffectSkipMount(() => {
        openSession(jobs.openInteractiveSession({id: jobId, rank: parseInt(rank, 10), sessionType: "SHELL"}));
    }, [jobId, rank]);

    const sessionWithProvider = sessionResp.data.sessions.length > 0 ? sessionResp.data.sessions[0] : null;
    let sessionIdentifier: string | null = null;
    if (sessionWithProvider?.session?.type === "shell") {
        sessionIdentifier = sessionWithProvider.session.sessionIdentifier;
    }

    useEffect(() => {
        if (sessionIdentifier === null || sessionWithProvider === null) return;
        if (termRef.current === null) return;
        setClosed(false);

        const wsConnection = WSFactory.open(
            `${sessionWithProvider.providerDomain}/ucloud/${sessionWithProvider.providerId}/compute/jobs/shells`,
            {
                reconnect: false,
                includeAuthentication: false,
                init: async conn => {
                    await conn.subscribe({
                        call: `jobs.compute.${sessionWithProvider.providerId}.shell.open`,
                        payload: {
                            type: "initialize",
                            sessionIdentifier,
                            cols: terminal.cols,
                            rows: terminal.rows
                        },
                        handler: message => {
                            if (message.type === "message") {
                                const payload = message.payload as { data: string } | any;
                                if ("data" in payload) {
                                    terminal.write(payload.data);
                                }
                            }
                        }
                    });
                },
                onClose: () => {
                    setClosed(true);
                },
            });

        terminal.onData((data) => {
            wsConnection.call({
                call: `jobs.compute.${sessionWithProvider.providerId}.shell.open`,
                payload: {
                    type: "input",
                    data
                }
            });
        });

        terminal.onResize((dims) => {
            wsConnection.call({
                call: `jobs.compute.${sessionWithProvider.providerId}.shell.open`,
                payload: {
                    type: "resize",
                    ...dims
                }
            });
        });

        fitAddon.fit();

        const resizeListener = (): void => {
            fitAddon.fit();
        };
        window.addEventListener("resize", resizeListener);

        return () => {
            wsConnection.close();
            window.removeEventListener("resize", resizeListener);
        };
    }, [termRef.current, sessionIdentifier, reconnect]);

    return <TermAndShellWrapper className={isLightThemeStored() ? "light" : "dark"} addPadding={true}>
        {!closed ? null : (
            // NOTE(Dan): Theme cannot change in practice, as a result we can safely use the stored value
            <Box className={`warn`}>
                <Box flexGrow={1}>Your connection has been closed!</Box>
                <Button ml={"16px"} onClick={() => {
                    setReconnect(reconnect + 1);
                }}>Reconnect</Button>
            </Box>
        )}

        <div className={"contents"} ref={termRef}/>
    </TermAndShellWrapper>;
};

export default Shell;