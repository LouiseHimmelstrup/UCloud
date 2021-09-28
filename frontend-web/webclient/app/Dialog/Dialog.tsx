import * as React from "react";
import {useEffect, useState} from "react";
import {default as ReactModal} from "react-modal";
import {dialogStore, Dialog as IDialog} from "@/Dialog/DialogStore";

export const Dialog: React.FunctionComponent = (): JSX.Element | null => {
    const [dialogs, setDialogs] = useState<IDialog[]>([]);

    useEffect(() => {
        const subscription = (dialogs: IDialog[]): void => setDialogs(dialogs);

        dialogStore.subscribe(subscription);
        return () => dialogStore.unsubscribe(subscription);
    }, []);

    const current = dialogs.length > 0 ? dialogs[0] : null;
    return (
        <ReactModal
            isOpen={current != null}
            shouldCloseOnEsc
            ariaHideApp={false}
            onRequestClose={() => dialogStore.failure()}
            onAfterOpen={() => undefined}
            style={current?.style ?? {
                content: {
                    top: "50%",
                    left: "50%",
                    right: "auto",
                    bottom: "auto",
                    marginRight: "-50%",
                    transform: "translate(-50%, -50%)",
                    background: "",
                    overflow: "visible"
                }
            }}
        >
            {current?.element ?? null}
        </ReactModal>
    );
};

export default Dialog;
