import * as React from "react";
import {Operation} from "ui-components/Operation";
import {useCallback, useEffect, useMemo, useState} from "react";
import {useToggleSet} from "Utilities/ToggleSet";
import {ListRowStat} from "ui-components/List";
import {History as MetadataHistory} from "./History";
import ReactModal from "react-modal";
import {largeModalStyle} from "Utilities/ModalUtilities";
import {FileMetadataDocumentOrDeleted, FileMetadataHistory} from "UCloud/MetadataDocumentApi";
import {FileMetadataTemplate} from "UCloud/MetadataNamespaceApi";
import {MetadataNamespacesBrowse} from "Files/Metadata/Templates/Namespaces";
import {ItemRenderer, StandardCallbacks, StandardList} from "ui-components/Browse";
import {SvgFt} from "ui-components/FtIcon";
import {getCssVar} from "Utilities/StyledComponentsUtilities";
import {noopCall} from "Authentication/DataHook";
import {useTraceUpdate} from "UtilityComponents";
import {UFile} from "UCloud/FilesApi";

export const entityName = "Metadata";

export const MetadataBrowse: React.FunctionComponent<{
    file: UFile;
    metadata: FileMetadataHistory;
    reload: () => void;
}> = ({file, metadata, reload}) => {
    const [lookingForTemplate, setLookingForTemplate] = useState<boolean>(false);
    const [inspecting, setInspecting] = useState<string | null>(null);
    const [creatingForTemplate, setCreatingForTemplate] = useState<FileMetadataTemplate | null>(null);
    const toggleSet = useToggleSet(Object.keys(metadata));

    const callbacks: Callbacks = useMemo(() => ({
        metadata,
        inspecting,
        setInspecting,
        setLookingForTemplate
    }), [inspecting, setInspecting, setLookingForTemplate, metadata]);

    const selectTemplate = useCallback((template: FileMetadataTemplate) => {
        setCreatingForTemplate(template);
        setLookingForTemplate(false);
    }, []);

    useEffect(() => {
        toggleSet.uncheckAll();
    }, [metadata]);

    const rows: MetadataRow[] = useMemo(() => {
        const rows: MetadataRow[] = [];
        Object.entries(metadata.metadata).forEach(([key, docs]) => {
            rows.push({
                key,
                docs,
                template: metadata.templates[key]
            });
        });
        return rows;
    }, [metadata]);

    const onNavigate = useCallback((row: MetadataRow) => {
        setInspecting(row.key);
    }, []);

    if (inspecting) {
        return <MetadataHistory metadata={metadata} reload={reload} template={metadata.templates[inspecting]}
            file={file} close={() => setInspecting(null)} />;
    } else if (creatingForTemplate != null) {
        return <MetadataHistory metadata={metadata} reload={reload} template={creatingForTemplate} file={file}
            close={() => setCreatingForTemplate(null)} />;
    }

    return <div>
        <StandardList
            generateCall={noopCall} renderer={fileMetadataRenderer} operations={operations}
            embedded={"inline"} preloadedResources={rows} title={entityName} titlePlural={entityName}
            extraCallbacks={callbacks} navigate={onNavigate}
        />
        <ReactModal
            isOpen={lookingForTemplate}
            ariaHideApp={false}
            shouldCloseOnEsc={true}
            onRequestClose={() => setLookingForTemplate(false)}
            style={largeModalStyle}
        >
            <MetadataNamespacesBrowse embedded={true} onTemplateSelect={selectTemplate} />
        </ReactModal>
    </div>;
};

interface MetadataRow {
    key: string;
    docs: FileMetadataDocumentOrDeleted[];
    template: FileMetadataTemplate;
}

const fileMetadataRenderer: ItemRenderer<MetadataRow> = {
    Icon({size}) {
        return <SvgFt width={size} height={size} type={"text"} ext={"meta"}
            color={getCssVar("FtIconColor")} color2={getCssVar("FtIconColor2")}
            hasExt={true} />
    },
    MainTitle({resource}) { return !resource ? null : <>{resource.template.title}</>},
    Stats({resource}) {
        if (!resource) return null;
        const allApproved = resource.docs.every(it =>
            it.type === "deleted" ||
            (
                it.type === "metadata" &&
                (it.status.approval.type === "not_required" || it.status.approval.type === "approved")
            )
        );
        return <>
            <ListRowStat icon={"hourglass"}
                children={`${resource.docs.length} version` + (resource.docs.length > 1 ? "s" : "")} />
            {allApproved ?
                <ListRowStat textColor={"green"} color={"green"} icon={"verified"}
                    children={"All entries approved"} />
                :
                <ListRowStat textColor={"red"} color={"red"} icon={"verified"}
                    children={"Updates are pending approval"} />
            }
        </>;
    }
};

interface Callbacks {
    metadata: FileMetadataHistory;
    inspecting: string | null;
    setInspecting: (inspecting: string | null) => void;
    setLookingForTemplate: (looking: boolean) => void;
}

const operations: Operation<MetadataRow, StandardCallbacks<MetadataRow> & Callbacks>[] = [
    {
        text: `Add ${entityName.toLowerCase()}`,
        primary: true,
        icon: "docs",
        enabled: (selected, cb) => selected.length === 0 && cb.inspecting == null,
        onClick: (_, cb) => {
            cb.setLookingForTemplate(true);
        }
    },
    {
        text: "Properties",
        icon: "properties",
        enabled: (selected) => selected.length === 1,
        onClick: (selected, cb) => {
            cb.setInspecting(selected[0].key);
        }
    }
];