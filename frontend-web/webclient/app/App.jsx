/*!
 *
 * Centric - Bootstrap Admin App + ReactJS
 *
 * Version: 1.9.5
 * Author: @themicon_co
 * Website: http://themicon.co
 * License: https://wrapbootstrap.com/help/licenses
 *
 */

import React from 'react';
import ReactDOM from 'react-dom';
//import {Router, Route, Link, IndexRoute, useRouterHistory} from 'react-router';
import {createBrowserHistory} from 'history'
import { BrowserRouter, Route } from "react-router-dom";

import "uppy/src/scss/uppy.scss";
import "./SiteComponents/Datatable.scss";
import "./SiteComponents/Common/Common";
import "./components/Colors/Colors"
import "./SiteComponents/Common/Utils.scss";
import "./components/Bootstrap/Bootstrap";
import Core from './SiteComponents/Core';
import Dashboard from "./SiteComponents/Dashboard";
import Notifications from "./SiteComponents/Activity/Notifications.jsx";
import Applications from "./SiteComponents/Applications/Applications";
import RunApp from "./SiteComponents/Applications/RunApp";
import Workflows from "./SiteComponents/Applications/Workflows";
import Analyses from "./SiteComponents/Applications/Analyses";
import Status from "./SiteComponents/StatusPage";
import Files from './SiteComponents/Files';
import FileInfo from "./SiteComponents/FileInfo";
import UserAuditing from "./SiteComponents/Admin/UserAuditing";
import { Cloud } from "../authentication/SDUCloudObject";


$(() => {
    Cloud.receiveAccessTokenOrRefreshIt();
    // prevent page reload when using dummy anchors
    $(document).on('click', '[href=""],[href="#"]', () => {
        return false;
    });

    // Support for float labels on inputs
    $(document).on('change', '.mda-form-control > input', function () {
        $(this)[this.value.length ? 'addClass' : 'removeClass']('has-value');
    });

});

const browserHistory = createBrowserHistory();

// Declare routes
ReactDOM.render(
    <BrowserRouter basename="app">
        <Core/>
    </BrowserRouter>,
    document.getElementById('app')
);

// Auto close sidebar on route changes
browserHistory.listen(function (ev) {
    $('.sidebar-visible').removeClass('sidebar-visible');
});