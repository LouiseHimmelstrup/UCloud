import * as React from "react";
import { Cloud } from "Authentication/SDUCloudObject";
import { Box, Button, TextArea, Error as UIError } from "ui-components";
import { MainContainer } from "MainContainer/MainContainer";

export class ErrorBoundary extends React.Component<{}, { hasError: boolean, error?: Error, submissionError?: string, errorInfo?: React.ErrorInfo }> {

    private ref = React.createRef<HTMLTextAreaElement>();

    constructor(props: Readonly<{}>) {
        super(props);
        this.state = {
            submissionError: undefined,
            hasError: false
        }
    }

    public static getDerivedStateFromError() {
        return { hasError: true, errorSent: false }
    }

    public componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
        this.setState(() => ({ error, errorInfo }));
    }

    private submitError = async () => {
        const { error, errorInfo } = this.state;
        const textAreaContent = this.ref.current ? this.ref.current.value : "None";
        try {
            await Cloud.post("/support/ticket", {
                message: `ERROR: ${error},\nSTACK: ${errorInfo!.componentStack},\nPathname: ${window.location.pathname},\nAdditional info: ${textAreaContent}`
            })
        } catch (e) {
            this.setState(() => ({ submissionError: !!e.response.why ? e.response.why : "An error occurred" }));
        }
        ErrorBoundary.redirectToDashboard();
    };

    private static redirectToDashboard() {
        Cloud.openLandingPage();
    }

    render() {
        if (this.state.hasError) {
            return (<MainContainer main={<Box maxWidth="435px" width="100%">
                <UIError error={this.state.submissionError} clearError={() => this.setState(() => ({ submissionError: undefined }))} />
                <Box>An error occurred. Would you like to submit an error report?</Box>
                <Box mb="0.5em"><TextArea placeholder="Please enter any information regarding the action you performed that caused an error" rows={5} width="100%" ref={this.ref} /></Box>
                <Button mr="1em" onClick={this.submitError} color="blue">Submit</Button><Button onClick={ErrorBoundary.redirectToDashboard}>Go to dashboard</Button>
            </Box>} />)
        }

        return this.props.children;
    }
}