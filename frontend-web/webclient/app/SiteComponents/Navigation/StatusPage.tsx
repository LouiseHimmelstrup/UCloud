import * as React from "react";
import { connect } from "react-redux";
import { updatePageTitle } from "../../Actions/Status";
import { SemanticCOLORS, Segment } from "semantic-ui-react";

const Status = ({ status, updatePageTitle }) => {
    updatePageTitle();
    return (
        <React.StrictMode>
            <Segment size="huge" content={status.title} color={levelToColor(status.level)} />
            <Segment padded="very" content={status.body} />
        </React.StrictMode>
    );
};

const levelToColor = (level: string): SemanticCOLORS => {
    switch (level) {
        case "NO ISSUES":
            return "green";
        case "MAINTENANCE":
        case "UPCOMING MAINTENANCE":
            return "yellow";
        case "ERROR":
            return "red";
    }
}

const mapDispatchToProps = (dispatch) => ({ updatePageTitle: () => dispatch(updatePageTitle("System Status")) });
const mapStateToProps = (state) => ({ status: state.status.status });
export default connect(mapStateToProps, mapDispatchToProps)(Status);