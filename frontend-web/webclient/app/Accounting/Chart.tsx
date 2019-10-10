import * as React from "react";
import {CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis} from "recharts";
import styled from "styled-components";
import * as API from "./api";
import * as DataTypes from "./DataTypes";
import * as MockedChart from "./mock/chart.json";

interface ChartProps {
    chart?: API.Chart<API.DataPoint2D>
}

function getOrElse<T>(idx: number, otherwise: T, array?: Array<T | null>): T {
    if (array === null || array === undefined) return otherwise;
    if (array.length < idx + 1) return otherwise;
    const element = array[idx];
    if (element === null) return otherwise;
    return element;
}

function Chart(props: ChartProps) {

    const chart: API.Chart<API.DataPoint2D> = props.chart || MockedChart.chart;

    const normalizedData = chart.data.map(d => {
        const xType = getOrElse(0, DataTypes.NUMBER, chart.dataTypes);
        const result: {name: string, value: any} = {
            name: API.formatDataType(xType, d.x),
            value: d.y
        };

        return result;
    });

    return (
        <Container aspect={16 / 9} maxHeight={576}>
            <LineChart data={normalizedData}>
                <CartesianGrid strokeDasharray="3 3" strokeWidth="2px" />
                <XAxis
                    dataKey="name"
                    tickCount={10}
                    axisLine={{strokeWidth: "2px"}}
                />

                <YAxis
                    dataKey="value"
                    tickFormatter={(d: number) =>
                        API.formatDataType(getOrElse(1, DataTypes.NUMBER, chart.dataTypes), d)}
                    axisLine={{strokeWidth: "2px"}}
                />

                <Tooltip
                    formatter={(d: number) =>
                        API.formatDataType(getOrElse(1, DataTypes.NUMBER, chart.dataTypes), d)}
                />
                <Legend />
                <Line
                    type="monotone"
                    stroke="#006aff"
                    dataKey="value"
                    strokeWidth="3px"
                    name={chart.dataTitle || "Value"}
                />
            </LineChart>
        </Container>
    );
}


const Container = styled(ResponsiveContainer)`
    & > div > svg {
        overflow: visible
    }
`;
export default Chart;
