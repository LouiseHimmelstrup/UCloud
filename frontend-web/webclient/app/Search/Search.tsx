import * as React from "react";
import * as Pagination from "Pagination";
import { connect } from "react-redux";
import { ApplicationCard } from "Applications/Card";
import { SearchItem } from "Project/Search";
import { AllFileOperations, favoriteFileFromPage } from "Utilities/FileUtilities";
import { SearchProps, SimpleSearchOperations, SimpleSearchStateProps } from ".";
import { HeaderSearchType, ReduxObject, emptyPage } from "DefaultObjects";
import { setPrioritizedSearch, setRefreshFunction } from "Navigation/Redux/HeaderActions";
import { Dispatch } from "redux";
import { SortOrder, SortBy, AdvancedSearchRequest, FileType } from "Files";
import * as SSActions from "./Redux/SearchActions";
import Error from "ui-components/Error";
import Text from "ui-components/Text";
import Flex from "ui-components/Flex";
import Hide from "ui-components/Hide";
import theme from "ui-components/theme";
import Input from "ui-components/Input";
import { MainContainer } from "MainContainer/MainContainer";
import { toggleFilesSearchHidden, setFilename } from "Files/Redux/DetailedFileSearchActions";
import { setAppName } from "Applications/Redux/DetailedApplicationSearchActions";
import { FilesTable } from "Files/FilesTable";
import { searchPage } from "Utilities/SearchUtilities";
import { getQueryParamOrElse } from "Utilities/URIUtilities";
import styled from "styled-components";
import { GridCardGroup } from "ui-components/Grid";
import { SidebarPages } from "ui-components/Sidebar";
import { setActivePage } from "Navigation/Redux/StatusActions";
import { Spacer } from "ui-components/Spacer";
import { Cloud } from "Authentication/SDUCloudObject";

interface SearchPane {
    headerType: HeaderSearchType
    menuItem: string
    render: () => JSX.Element
}

class Search extends React.Component<SearchProps> {

    componentDidMount() {
        this.props.toggleAdvancedSearch();
        this.props.setActivePage();

        const query = this.query;
        this.props.setSearch(query);
        this.props.setPrioritizedSearch(this.props.match.params.priority as HeaderSearchType);
        this.fetchAll(query);
        this.props.setRefresh(() => this.fetchAll(query));
    }

    componentWillReceiveProps() {
        this.props.setRefresh(() => this.fetchAll(this.query));
    }

    queryFromProps = (props: SearchProps): string => {
        return getQueryParamOrElse(props, "query", "");
    }

    get query(): string {
        return this.queryFromProps(this.props);
    }

    get fileSearchBody(): AdvancedSearchRequest {
        // FIXME Duplicate code
        const { ...fileSearch } = this.props.fileSearch;
        let fileTypes: [FileType?, FileType?] = [];
        if (fileSearch.allowFiles) fileTypes.push("FILE");
        if (fileSearch.allowFolders) fileTypes.push("DIRECTORY");
        const createdAt = {
            after: !!fileSearch.createdAfter ? fileSearch.createdAfter.valueOf() : undefined,
            before: !!fileSearch.createdBefore ? fileSearch.createdBefore.valueOf() : undefined,
        };
        const modifiedAt = {
            after: !!fileSearch.modifiedAfter ? fileSearch.modifiedAfter.valueOf() : undefined,
            before: !!fileSearch.modifiedBefore ? fileSearch.modifiedBefore.valueOf() : undefined,
        };

        return {
            fileName: !!fileSearch.fileName ? fileSearch.fileName : this.query,
            extensions: [...fileSearch.extensions],
            fileTypes,
            createdAt: typeof createdAt.after === "number" || typeof createdAt.before === "number" ? createdAt : undefined,
            modifiedAt: typeof modifiedAt.after === "number" || typeof modifiedAt.before === "number" ? modifiedAt : undefined,
            itemsPerPage: this.props.files.itemsPerPage || 25,
            page: 0
        }
    }

    componentWillUnmount() {
        this.props.toggleAdvancedSearch();
        this.props.clear();
        this.props.setRefresh();
    }

    shouldComponentUpdate(nextProps: SearchProps): boolean {
        // TODO It seems like a bad idea to perform side-effects in this method!

        const currentQuery = this.query;
        const nextQuery = this.queryFromProps(nextProps);
        if (nextQuery !== currentQuery) {
            this.props.setSearch(nextQuery);
            this.fetchAll(nextQuery);
            return false;
        }
        if (nextProps.match.params.priority !== this.props.match.params.priority) {
            this.props.setPrioritizedSearch(nextProps.match.params.priority as HeaderSearchType);
            this.fetchAll(nextQuery);
            return false;
        }
        return true;
    }

    setPath = (text: string) => {
        this.props.setPrioritizedSearch(text as HeaderSearchType);
        this.props.history.push(searchPage(text.toLocaleLowerCase(), this.props.search));
    }

    fetchAll(search: string, itemsPerPage?: number) {
        const { props } = this;
        props.setError();
        props.searchFiles({ ...this.fileSearchBody, fileName: search, itemsPerPage: itemsPerPage || this.props.files.itemsPerPage });
        props.searchApplications(search, this.props.applications.pageNumber, itemsPerPage || this.props.applications.itemsPerPage);
        props.searchProjects(search, this.props.projects.pageNumber, itemsPerPage || this.props.projects.itemsPerPage);
    }

    search() {
        if (!this.props.search) return;
        this.props.history.push(searchPage(this.props.match.params.priority, this.props.search));
    }

    render() {
        const refreshFiles = () => this.props.searchFiles({ ...this.fileSearchBody })
        const { search, files, projects, applications, filesLoading, applicationsLoading, projectsLoading, errors } = this.props;
        const fileOperations = AllFileOperations({
            stateless: true,
            history: this.props.history,
            onDeleted: () => refreshFiles(),
            onExtracted: () => refreshFiles(),
            onLinkCreate: () => refreshFiles(),
            setLoading: () => this.props.setFilesLoading(true)
        });
        // FIXME: Search Pane approach is obsolete
        const panes: SearchPane[] = [
            {
                headerType: "files",
                menuItem: "Files",
                render: () => (
                    <Pagination.List
                        loading={filesLoading}
                        pageRenderer={page => (
                            <FilesTable
                                files={page.items}
                                sortOrder={SortOrder.ASCENDING}
                                sortingColumns={[SortBy.MODIFIED_AT, SortBy.SENSITIVITY]}
                                sortFiles={() => undefined}
                                onCheckFile={() => undefined}
                                refetchFiles={() => this.props.searchFiles(this.fileSearchBody)}
                                sortBy={SortBy.PATH}
                                onFavoriteFile={files => this.props.setFilesPage(favoriteFileFromPage(this.props.files, files, Cloud))}
                                fileOperations={fileOperations}
                            />
                        )}
                        page={files}
                        onPageChanged={pageNumber => this.props.searchFiles({ ...this.fileSearchBody, page: pageNumber })}
                    />
                )
            },
            {
                headerType: "projects",
                menuItem: "Projects",
                render: () => (
                    <Pagination.List
                        loading={projectsLoading}
                        pageRenderer={page => page.items.map((it, i) => (<SearchItem key={i} item={it} />))}
                        page={projects}
                        onPageChanged={pageNumber => this.props.searchProjects(search, pageNumber, projects.itemsPerPage)}
                    />
                )
            },
            {
                headerType: "applications",
                menuItem: "Applications",
                render: () => (
                    <Pagination.List
                        loading={applicationsLoading}
                        pageRenderer={({ items }) =>
                            <GridCardGroup>
                                {items.map(app =>
                                    <ApplicationCard
                                        key={`${app.metadata.name}${app.metadata.version}`}
                                        app={app}
                                        isFavorite={app.favorite}
                                    />)}
                            </GridCardGroup>
                        }
                        page={applications}
                        onPageChanged={(pageNumber) => this.props.searchApplications(search, pageNumber, applications.itemsPerPage)}
                    />
                )
            }
        ];
        const activeIndex = SearchPriorityToNumber(this.props.match.params.priority);

        const Tab = ({ pane, index }: { pane: SearchPane, index: number }): JSX.Element => (
            <SelectableText
                key={index}
                cursor="pointer"
                fontSize={2}
                onClick={() => this.setPath(pane.headerType)}
                selected={activeIndex === index}
                mr="1em"
            >
                {pane.menuItem}
            </SelectableText>
        );
        return (
            <MainContainer
                header={
                    <React.Fragment>
                        <Error error={errors.join("\n")} clearError={() => this.props.setError(undefined)} />
                        <Hide xxl xl md>
                            <form onSubmit={e => (e.preventDefault(), this.search())}>
                                <Input onChange={({ target: { value } }) => this.props.setSearch(value)} />
                            </form>
                        </Hide>
                        <SearchOptions>
                            {panes.map((pane, index) => <Tab pane={pane} index={index} key={index} />)}
                        </SearchOptions>
                        <Spacer left={null} right={<Pagination.EntriesPerPageSelector
                            onChange={itemsPerPage => this.fetchAll(this.props.search, itemsPerPage)}
                            content={`${panes[activeIndex].menuItem} per page`}
                            entriesPerPage={
                                activeIndex === 0 ? this.props.files.itemsPerPage : (
                                    activeIndex === 1 ? this.props.projects.itemsPerPage :
                                        this.props.applications.itemsPerPage)
                            }
                        />} />
                    </React.Fragment>
                }
                main={panes[activeIndex].render()}
            />
        );
    }
};

export const SearchOptions = styled(Flex)`
    border-bottom: 1px solid ${theme.colors.lightGray};
`;

SearchOptions.defaultProps = {
    theme
}

export const SelectableText = styled(Text) <{ selected: boolean }>`
    border-bottom: ${props => props.selected ? `2px solid ${theme.colors.blue}` : ""};
`;

SelectableText.defaultProps = {
    theme
}

const SearchPriorityToNumber = (search: string): number => {
    if (search.toLocaleLowerCase() === "projects") return 1;
    else if (search.toLocaleLowerCase() === "applications") return 2;
    return 0;
}

const mapDispatchToProps = (dispatch: Dispatch): SimpleSearchOperations => ({
    setFilesLoading: loading => dispatch(SSActions.setFilesLoading(loading)),
    setApplicationsLoading: loading => dispatch(SSActions.setApplicationsLoading(loading)),
    setProjectsLoading: loading => dispatch(SSActions.setProjectsLoading(loading)),
    setError: error => dispatch(SSActions.setErrorMessage(error)),
    clear: () => {
        dispatch(SSActions.receiveFiles(emptyPage))
        dispatch(SSActions.receiveFiles(emptyPage))
        dispatch(SSActions.receiveProjects(emptyPage))
    },
    searchFiles: async (body) => {
        dispatch(SSActions.setFilesLoading(true));
        dispatch(await SSActions.searchFiles(body));
        dispatch(setFilename(body.fileName || ""));
    },
    searchApplications: async (query, page, itemsPerPage) => {
        dispatch(SSActions.setApplicationsLoading(true));
        dispatch(await SSActions.searchApplications(query, page, itemsPerPage));
        dispatch(setAppName(query));
    },
    searchProjects: async (query, page, itemsPerPage) => {
        dispatch(SSActions.setProjectsLoading(true));
        dispatch(await SSActions.searchProjects(query, page, itemsPerPage));
    },
    setFilesPage: page => dispatch(SSActions.receiveFiles(page)),
    setApplicationsPage: page => dispatch(SSActions.receiveApplications(page)),
    setProjectsPage: page => dispatch(SSActions.receiveProjects(page)),
    setSearch: search => dispatch(SSActions.setSearch(search)),
    setPrioritizedSearch: sT => dispatch(setPrioritizedSearch(sT)),
    toggleAdvancedSearch: () => dispatch(toggleFilesSearchHidden()),
    setActivePage: () => dispatch(setActivePage(SidebarPages.None)),
    setRefresh: refresh => dispatch(setRefreshFunction(refresh))
});

const mapStateToProps = ({ simpleSearch, detailedFileSearch, detailedApplicationSearch }: ReduxObject): SimpleSearchStateProps & { favFilesCount: number } => ({
    ...simpleSearch,
    favFilesCount: simpleSearch.files.items.filter(it => it.favorited).length,
    fileSearch: detailedFileSearch,
    applicationSearch: detailedApplicationSearch
});

export default connect(mapStateToProps, mapDispatchToProps)(Search)