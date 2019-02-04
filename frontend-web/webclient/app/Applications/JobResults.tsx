import * as React from "react";
import { toLowerCaseAndCapitalize, shortUUID } from "UtilityFunctions"
import { updatePageTitle, setActivePage } from "Navigation/Redux/StatusActions";
import { Cloud } from "Authentication/SDUCloudObject";
import { Link } from "ui-components";
import { List } from "Pagination/List";
import { connect } from "react-redux";
import { setLoading, fetchAnalyses } from "./Redux/AnalysesActions";
import { AnalysesProps, AnalysesState, AnalysesOperations, AnalysesStateProps } from ".";
import { setErrorMessage } from "./Redux/AnalysesActions";
import { Dispatch } from "redux";
import { Table, TableBody, TableCell, TableHeader, TableHeaderCell, TableRow } from "ui-components/Table";
import { fileTablePage } from "Utilities/FileUtilities";
import { MainContainer } from "MainContainer/MainContainer";
import { History } from "history";
import { ReduxObject } from "DefaultObjects";
import { SidebarPages } from "ui-components/Sidebar";
import * as Heading from "ui-components/Heading";
import { setRefreshFunction } from "Navigation/Redux/HeaderActions";

class JobResults extends React.Component<AnalysesProps & { history: History }, AnalysesState> {
    constructor(props: Readonly<AnalysesProps & { history: History }>) {
        super(props);
        this.state = {
            reloadIntervalId: -1
        };
        props.setActivePage();
        props.updatePageTitle("Results");
    }

    componentDidMount() {
        this.getAnalyses(false);
        let reloadIntervalId = window.setInterval(() => {
            this.getAnalyses(true)
        }, 10_000);
        this.setState({ reloadIntervalId });
        const { setRefresh } = this.props;
        setRefresh(() => this.getAnalyses(false));
    }

    componentWillReceiveProps(nextProps: AnalysesProps) {
        const { setRefresh } = nextProps;
        setRefresh(() => this.getAnalyses(false));
    }

    componentWillUnmount() {
        clearInterval(this.state.reloadIntervalId);
        this.props.setRefresh();
    }

    getAnalyses(silent: boolean) {
        const { page } = this.props;
        if (!silent) this.props.setLoading(true);
        this.props.fetchAnalyses(page.itemsPerPage, page.pageNumber);
    }

    render() {
        const { page, loading, fetchAnalyses, error, onErrorDismiss, history } = this.props;
        const content = <List
            customEmptyPage={<Heading.h1>No jobs have been run on this account.</Heading.h1>}
            loading={loading}
            onErrorDismiss={onErrorDismiss}
            errorMessage={error}
            customEntriesPerPage
            pageRenderer={(page) =>
                <Table>
                    <Header />
                    <TableBody>
                        {page.items.map((a, i) => <Analysis
                            to={() => history.push(`/applications/results/${a.jobId}`)} analysis={a} key={i}
                        />)}
                    </TableBody>
                </Table>
            }
            page={page}
            onPageChanged={pageNumber => fetchAnalyses(page.itemsPerPage, pageNumber)}
        />;

        return (<MainContainer
            header={<Heading.h1>Job Results</Heading.h1>}
            main={content}
        />);
    }
}

const Header = () => (
    <TableHeader>
        <TableRow>
            <TableHeaderCell textAlign="left">App Name</TableHeaderCell>
            <TableHeaderCell textAlign="left">Job Id</TableHeaderCell>
            <TableHeaderCell textAlign="left">State</TableHeaderCell>
            <TableHeaderCell textAlign="left">Status</TableHeaderCell>
            <TableHeaderCell textAlign="left">Started at</TableHeaderCell>
            <TableHeaderCell textAlign="left">Last updated at</TableHeaderCell>
        </TableRow>
    </TableHeader>
);

// FIXME: Typesafety. But how has this worked setting Link as title?
const Analysis = ({ analysis, to }) => {
    const jobIdField = analysis.status === "COMPLETE" ?
        (<Link to={fileTablePage(`${Cloud.jobFolder}/${analysis.jobId}`)}>{analysis.jobId}</Link>) : analysis.jobId;
    return (
        <TableRow cursor="pointer" onClick={() => to()}>
            <TableCell>{analysis.appName}@{analysis.appVersion}</TableCell>
            <TableCell><span title={jobIdField}>{shortUUID(jobIdField)}</span></TableCell>
            <TableCell>{toLowerCaseAndCapitalize(analysis.state)}</TableCell>
            <TableCell>{analysis.status}</TableCell>
            <TableCell>{formatDate(analysis.createdAt)}</TableCell>
            <TableCell>{formatDate(analysis.modifiedAt)}</TableCell>
        </TableRow>)
};

const formatDate = (millis: number) => {
    let d = new Date(millis);
    return `${pad(d.getDate(), 2)}/${pad(d.getMonth() + 1, 2)}/` +
        `${pad(d.getFullYear(), 2)} ${pad(d.getHours(), 2)}:` +
        `${pad(d.getMinutes(), 2)}:${pad(d.getSeconds(), 2)}`;
};

const pad = (value: string | number, length: number) =>
    (value.toString().length < length) ? pad("0" + value, length) : value;

const mapDispatchToProps = (dispatch: Dispatch): AnalysesOperations => ({
    onErrorDismiss: () => dispatch(setErrorMessage(undefined)),
    updatePageTitle: () => dispatch(updatePageTitle("Results")),
    setLoading: loading => dispatch(setLoading(loading)),
    fetchAnalyses: async (itemsPerPage, pageNumber) => dispatch(await fetchAnalyses(itemsPerPage, pageNumber)),
    setActivePage: () => dispatch(setActivePage(SidebarPages.MyResults)),
    setRefresh: refresh => dispatch(setRefreshFunction(refresh))
});

const mapStateToProps = ({ analyses }: ReduxObject): AnalysesStateProps => analyses;
export default connect(mapStateToProps, mapDispatchToProps)(JobResults);