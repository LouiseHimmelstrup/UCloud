import {useCloudCommand} from "Authentication/DataHook";
import * as React from "react";
import {extensionFromPath, extensionTypeFromPath, isExtPreviewSupported} from "UtilityFunctions";
import {PredicatedLoadingSpinner} from "LoadingIcon/LoadingIcon";
import {Flex, Markdown} from "ui-components";
import FilesApi, {FilesCreateDownloadResponseItem, UFile} from "UCloud/FilesApi";
import {useEffect, useState} from "react";
import {fileName} from "Utilities/FileUtilities";
import {bulkRequestOf} from "DefaultObjects";
import {BulkResponse} from "UCloud";
import SyntaxHighlighter from "react-syntax-highlighter";
import styled from "styled-components";

export const MAX_PREVIEW_SIZE_IN_BYTES = 50_000_000;

export const FilePreview: React.FunctionComponent<{ file: UFile }> = ({file}) => {
    const extension = extensionFromPath(file.id);
    const isValidExtension = isExtPreviewSupported(extension);
    const type = extensionTypeFromPath(file.id);
    const [loading, invokeCommand] = useCloudCommand();

    const [data, setData] = useState("");
    const [error, setError] = useState<string | null>(null);

    const fetchData = React.useCallback(async () => {
        const size = file.status.sizeInBytes;
        if (file.status.type !== "FILE") return;
        if (!loading && isValidExtension && size != null && size < MAX_PREVIEW_SIZE_IN_BYTES) {
            try {
                const download = await invokeCommand<BulkResponse<FilesCreateDownloadResponseItem>>(
                    FilesApi.createDownload(bulkRequestOf({id: file.id})),
                    {defaultErrorHandler: false}
                );
                const downloadEndpoint = download?.responses[0]?.endpoint;
                if (!downloadEndpoint) {
                    setError("Unable to display preview. Try again later or with a different file.");
                    return;
                }
                const content = await fetch(downloadEndpoint);
                switch (type) {
                    case "image":
                    case "audio":
                    case "video":
                    case "pdf":
                        setData(URL.createObjectURL(await content.blob()));
                        break;
                    case "code":
                    case "text":
                    case "application":
                    case "markdown":
                        setData(await content.text());
                        break;
                    default:
                        setError(`Preview not support for '${extensionFromPath(file.id)}'.`);
                        break;
                }
            } catch (e) {
                setError("Unable to display preview. Try again later or with a different file.");
            }
        } else if (size != null && size >= MAX_PREVIEW_SIZE_IN_BYTES) {
            setError("File is too large to preview");
        } else {
            setError("Preview is not supported for this file.");
        }
    }, [file]);

    useEffect(() => {
        fetchData();
    }, [file]);

    if (file.status.type !== "FILE") return null;
    if (loading) return <PredicatedLoadingSpinner loading/>

    let node: JSX.Element | null;

    console.log("data", data != null);
    switch (type) {
        case "text":
        case "code":
            /* Even 100_000 tanks performance. Anything above stalls or kills the sandbox process. */
            if (file.status.sizeInBytes == null || file.status.sizeInBytes > 100_000) {
                node = <div><pre className="fullscreen">{data}</pre></div>
            } else {
                node = <div><SyntaxHighlighter className="fullscreen">{data}</SyntaxHighlighter></div>;
            }
            break;
        case "image":
            node = <img alt={fileName(file.id)} src={data}/>
            break;
        case "audio":
            node = <audio controls src={data} />;
            break;
        case "video":
            node = <video src={data} controls />;
            break;
        case "pdf":
            node = <embed style={{width: "100vw", height: "100vh"}} className="fullscreen" src={data} />;
            break;
        case "markdown":
            node = <div><Markdown>{data}</Markdown></div>;
            break;
        default:
            node = <div/>
            break;
    }

    if (error !== null) {
        node = <div>{error}</div>;
    }

    return <ItemWrapper>{node}</ItemWrapper>;
}

const ItemWrapper = styled.div`
    display: flex;
    justify-content: center;
    margin-bottom: 30px;

    & > * {
      max-width: 100%;
      max-height: calc(100vh - 200px);
      overflow-y: scroll;
    }
`;