import {WSFactory} from "@/Authentication/HttpClientInstance";
import {Progress, Speed, Task, TaskUpdate} from "@/BackgroundTasks/api";
import DetailedTask from "@/BackgroundTasks/DetailedTask";
import * as React from "react";
import {useCallback, useEffect, useState} from "react";
import {default as ReactModal} from "react-modal";
import styled from "styled-components";
import {Icon} from "@/ui-components";
import Box from "@/ui-components/Box";
import ClickableDropdown from "@/ui-components/ClickableDropdown";
import Flex from "@/ui-components/Flex";
import IndeterminateProgressBar from "@/ui-components/IndeterminateProgress";
import ProgressBar from "@/ui-components/Progress";
import {defaultModalStyle} from "@/Utilities/ModalUtilities";
import {useCloudAPI} from "@/Authentication/DataHook";
import {emptyPage} from "@/DefaultObjects";
import {buildQueryString} from "@/Utilities/URIUtilities";
import {associateBy, takeLast} from "@/Utilities/CollectionUtilities";

function insertTimestamps(speeds: Speed[]): Speed[] {
    return speeds.map(it => {
        if (it.clientTimestamp) {
            return it;
        } else {
            return {...it, clientTimestamp: Date.now()};
        }
    });
}

const BackgroundTasks: React.FunctionComponent = () => {
    const [initialTasks, fetchInitialTasks] = useCloudAPI<Page<Task>>({noop: true}, emptyPage);
    const [taskInFocus, setTaskInFocus] = useState<string | null>(null);
    const [tasks, setTasks] = useState<Record<string, TaskUpdate>>({});

    const handleTaskUpdate = useCallback((update: TaskUpdate) => {
        setTasks(oldTasks => {
            const newTasks: Record<string, TaskUpdate> = {...oldTasks};
            const existingTask = newTasks[update.jobId];
            if (update.complete) {
                delete newTasks[update.jobId];
            } else if (!existingTask) {
                newTasks[update.jobId] = {
                    ...update,
                    speeds: insertTimestamps(update.speeds)
                };
            } else {
                const currentMessage = existingTask.messageToAppend ? existingTask.messageToAppend : "";
                const messageToAdd = update.messageToAppend ? update.messageToAppend : "";
                const newMessage = currentMessage + messageToAdd;

                const newStatus = update.newStatus ? update.newStatus : existingTask.newStatus;
                const newTitle = update.newTitle ? update.newTitle : existingTask.newTitle;
                const newProgress = update.progress ? update.progress : existingTask.progress;
                const newSpeed = takeLast((existingTask.speeds || []).concat(update.speeds || []), 500);
                const newComplete = update.complete ? update.complete : existingTask.complete;

                newTasks[update.jobId] = {
                    ...existingTask,
                    messageToAppend: newMessage,
                    progress: newProgress,
                    speeds: insertTimestamps(newSpeed),
                    complete: newComplete,
                    newStatus,
                    newTitle
                };
            }
            return newTasks;
        });
    }, [setTasks]);

    useEffect(() => {
        fetchInitialTasks({
            method: "GET",
            path: buildQueryString("/tasks", {itemsPerPage: 100, page: 0})
        });
    }, []);

    useEffect(() => {
        setTasks(associateBy(
            initialTasks.data.items.map(it => ({
                    jobId: it.jobId,
                    speeds: [],
                    messageToAppend: null,
                    progress: null,
                    newTitle: it.title,
                    complete: false,
                    newStatus: it.status
                }),
            ),
            it => it.jobId
        ));
    }, [initialTasks.data]);

    useEffect(() => {
        const wsConnection = WSFactory.open("/tasks", {
            init: async conn => {
                await conn.subscribe({
                    call: "task.listen",
                    payload: {},
                    handler: message => {
                        if (message.type === "message") {
                            const payload = message.payload as TaskUpdate;
                            handleTaskUpdate(payload);
                        }
                    }
                });
            }
        });

        return () => wsConnection.close();
    }, []);

    const onDetailedClose = useCallback(() => {
        setTaskInFocus(null);
    }, []);

    const hasTaskInFocus = taskInFocus && (tasks && tasks[taskInFocus]);
    const numberOfTasks = Object.keys(tasks).length;
    if (numberOfTasks === 0) return null;
    return (
        <>
            <ClickableDropdown
                width="600px"
                left="-400px"
                top="37px"
                trigger={<Flex justifyContent="center"><TasksIcon/></Flex>}
            >
                {!tasks ? null :
                    Object.values(tasks).map(update => (
                        <TaskComponent
                            key={update.jobId}
                            jobId={update.jobId}
                            onClick={setTaskInFocus}
                            title={update.newTitle ?? ""}
                            speed={!!update.speeds ? update.speeds[update.speeds.length - 1] : undefined}
                            progress={update.progress ? update.progress : undefined}
                        />
                    ))
                }
            </ClickableDropdown>

            <ReactModal
                style={defaultModalStyle}
                isOpen={!!hasTaskInFocus}
                onRequestClose={onDetailedClose}
                ariaHideApp={false}
            >
                {!hasTaskInFocus ? null : <DetailedTask task={tasks[taskInFocus!]!}/>}
            </ReactModal>
        </>
    );
};

interface TaskComponentProps {
    title: string;
    progress?: Progress;
    speed?: Speed;
    onClick?: (jobId: string) => void;
    jobId?: string;
}

const TaskComponent: React.FunctionComponent<TaskComponentProps> = props => {
    const label = props.speed?.asText ?? "";
    const onClickHandler = useCallback(
        () => {
            if (props.onClick && props.jobId) {
                props.onClick(props.jobId);
            }
        },
        [props.jobId, props.onClick]
    );

    return (
        <TaskContainer onClick={onClickHandler}>
            <Box mr={16}>
                <b>{props.title}</b>
            </Box>

            <Box flexGrow={1}>
                {!props.progress ?
                    <IndeterminateProgressBar color="green" label={label}/> :

                    (
                        <ProgressBar
                            active={true}
                            color="green"
                            label={label}
                            percent={(props.progress.current / props.progress.maximum) * 100}
                        />
                    )
                }
            </Box>
        </TaskContainer>
    );
};

const TaskContainer = styled(Flex)`
  padding: 16px;
  cursor: pointer;

  > * {
    cursor: inherit;
  }
`;

const TasksIconBase = styled(Icon)`
  animation: spin 2s linear infinite;
  margin-right: 8px;

  @keyframes spin {
    0% {
      transform: rotate(0deg);
    }
    100% {
      transform: rotate(360deg);
    }
  }
`;

const TasksIcon = (): JSX.Element => <TasksIconBase name="notchedCircle"/>;

export default BackgroundTasks;
