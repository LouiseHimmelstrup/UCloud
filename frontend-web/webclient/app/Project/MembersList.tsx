import {changeRoleInProject, ProjectMember, ProjectRole, projectRoleToString, transferPiRole, projectStringToRole} from "Project/index";
import {useAsyncCommand} from "Authentication/DataHook";
import {useAvatars} from "AvataaarLib/hook";
import * as React from "react";
import {useEffect} from "react";
import {defaultAvatar} from "UserSettings/Avataaar";
import {Flex, Icon, Text, Box, Button, RadioTile, RadioTilesContainer} from "ui-components";
import {IconName} from "ui-components/Icon";
import {snackbarStore} from "Snackbar/SnackbarStore";
import {errorMessageOrDefault} from "UtilityFunctions";
import {isAdminOrPI} from "Utilities/ProjectUtilities";
import {addStandardDialog} from "UtilityComponents";
import {UserAvatar} from "AvataaarLib/UserAvatar";
import {RemoveButton} from "Files/FileInputSelector";

export function MembersList(props: Readonly<{
    members: ProjectMember[];
    onAddToGroup?: (member: string) => void;
    onRemoveMember(member: string): void;
    allowRoleManagement: boolean;
    projectRole: ProjectRole;
    reload?: () => void;
    isOutgoingInvites?: boolean;
    showRole?: boolean;
    projectId: string;
}>): JSX.Element {
    const [, runCommand] = useAsyncCommand();
    const avatars = useAvatars();
    const allowManagement = isAdminOrPI(props.projectRole);

    useEffect(() => {
        const usernames = props.members.map(it => it.username);
        avatars.updateCache(usernames);
    }, [props.members]);

    const options: {text: string; icon: IconName; value: ProjectRole}[] = [
        {text: "User", icon: "user", value: ProjectRole.USER},
        {text: "Admin", icon: "userAdmin", value: ProjectRole.ADMIN}
    ];

    if (props.projectRole === ProjectRole.PI) {
        options.push({text: "PI", icon: "userPi", value: ProjectRole.PI});
    }

    return (<>
        {props.members.map(member =>
            <>
                <Flex alignItems="center" mb="16px">
                    <UserAvatar avatar={avatars.cache[member.username] ?? defaultAvatar} mr="10px" />
                    {!props.isOutgoingInvites ? <Text bold>{member.username}</Text> :
                        <div>
                            <Text bold>{member.username}</Text>
                            Invited to join
                        </div>
                    }

                    <Box flexGrow={1} />

                    {props.showRole === false ? null :
                        !props.allowRoleManagement || member.role === ProjectRole.PI ?
                            projectRoleToString(member.role)
                            :
                            <>
                                <RadioTilesContainer height="48px">
                                    {options.map(role =>
                                        <RadioTile key={role.text}
                                            height={40}
                                            labeled={true}
                                            label={role.text}
                                            fontSize={"0.5em"}
                                            icon={role.icon}
                                            checked={role.value === member.role}
                                            onChange={async event => {
                                                try {
                                                    if (event.currentTarget.value === "PI") {
                                                        addStandardDialog({
                                                            title: "Transfer PI Role",
                                                            message: "Are you sure you wish to transfer the PI role? " +
                                                                "A project can only have one PI. " +
                                                                "Your own user will be demoted to admin.",
                                                            onConfirm: async () => {
                                                                await runCommand(
                                                                    transferPiRole({ newPrincipalInvestigator: member.username })
                                                                );

                                                                if (props.reload) props.reload();
                                                            },
                                                            confirmText: "Transfer PI role"
                                                        });
                                                    } else {
                                                        await runCommand(changeRoleInProject({
                                                            projectId: props.projectId,
                                                            member: member.username,
                                                            newRole: projectStringToRole(event.currentTarget.value)
                                                        }));
                                                        if (props.reload) props.reload();
                                                    }
                                                } catch (err) {
                                                    snackbarStore.addFailure(
                                                        errorMessageOrDefault(err, "Failed to update role."), false
                                                    );
                                                }
                                            }}
                                        />
                                    )
                                    }
                                </RadioTilesContainer>
                            </>
                    }

                    <Flex alignItems={"center"}>
                        {!props.onAddToGroup ? null :
                            <Button color="green" height="35px" width="35px" onClick={() => props.onAddToGroup!(member.username)}>
                                <Icon
                                    color="white"
                                    name="arrowDown"
                                    rotation={270}
                                    width="1em"
                                    title="Add to group"
                                />
                            </Button>
                        }
                        {!allowManagement || member.role === ProjectRole.PI ? null :
                            <RemoveButton width="35px" height="35px" onClick={() => props.onRemoveMember(member.username)} />
                        }
                    </Flex>
                </Flex>
            </>
        )
        }
    </>);
}
