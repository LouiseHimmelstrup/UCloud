import * as React from "react";
import Box from "@/ui-components/Box";
import Flex from "@/ui-components/Flex";
import Icon from "@/ui-components/Icon";
import {ErrorWrapper} from "./Error";

interface WarningProps {
    clearWarning?: () => void;
    warning?: string;
    width?: string | number;
    children?: React.ReactNode
}

const Warning: React.FunctionComponent<WarningProps> = props => {
    if (!props.warning && !props.children) return null;

    function onClearWarning(e: React.MouseEvent<HTMLElement, MouseEvent>): void {
        props.clearWarning!();
        e.stopPropagation();
    }

    return (
        <ErrorWrapper
            bg="lightYellow"
            borderColor="yellow"
            width={props.width}
        >
            <Flex alignItems="center">
                <div style={{whiteSpace: "pre"}}>
                    {props.warning}
                    {props.children}
                </div>
                {!props.clearWarning ? null : (
                    <Box ml="auto">
                        <Icon
                            size="1em"
                            name="close"
                            color="black"
                            onClick={onClearWarning}
                        />
                    </Box>
                )}
            </Flex>
        </ErrorWrapper>
    );
};

export default Warning;

