import {configure, mount} from "enzyme";
import * as Adapter from "enzyme-adapter-react-16";
import * as React from "react";
import {create} from "react-test-renderer";
import {ThemeProvider} from "styled-components";
import {BreadCrumbs} from "../../app/ui-components/Breadcrumbs";
import theme from "../../app/ui-components/theme";
import {Client} from "../../app/Authentication/HttpClientInstance";
configure({adapter: new Adapter()});


describe("Breadcrumbs", () => {
    it("Build breadcrumbs, embedded", () => {
        expect(create(
            <ThemeProvider theme={theme}>
                <BreadCrumbs
                    embedded
                    currentPath="/home/mail@mailhost.dk/folder1"
                    navigate={() => undefined}
                    client={Client}
                />
            </ThemeProvider>)).toMatchSnapshot();
    });

    it("Build breadcrumbs with empty path, embedded", () => {
        expect(create(
            <ThemeProvider theme={theme}>
                <BreadCrumbs
                    embedded
                    currentPath=""
                    navigate={() => undefined}
                    client={Client}
                />
            </ThemeProvider>
        )).toMatchSnapshot();
    });

    it("Build breadcrumbs, unembedded", () => {
        expect(create(
            <ThemeProvider theme={theme}>
                <BreadCrumbs
                    embedded={false}
                    currentPath="/home/mail@mailhost.dk/folder1"
                    navigate={() => undefined}
                    client={Client}
                />
            </ThemeProvider>)).toMatchSnapshot();
    });

    it("Build breadcrumbs with empty path, unembedded", () => {
        expect(create(
            <ThemeProvider theme={theme}>
                <BreadCrumbs
                    embedded={false}
                    currentPath=""
                    navigate={() => undefined}
                    client={Client}
                />
            </ThemeProvider>
        )).toMatchSnapshot();
    });

    it("Using navigate", () => {
        const navigate = jest.fn();
        const breadcrumbs = mount(
            <ThemeProvider theme={theme}>
                <BreadCrumbs
                    embedded
                    currentPath="/home/mail@mailhost.dk/folder1"
                    navigate={navigate}
                    client={Client}
                />
            </ThemeProvider>
        );
        breadcrumbs.find("span").first().simulate("click");
        expect(navigate).toHaveBeenCalled();
    });
});
