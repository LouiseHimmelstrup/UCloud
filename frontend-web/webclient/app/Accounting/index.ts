import {buildQueryString} from "Utilities/URIUtilities";

export type AccountType = "USER" | "PROJECT";

export interface ProductCategoryId {
    id: string;
    provider: string;
    title?: string;
}

export enum ProductArea {
    COMPUTE = "COMPUTE",
    STORAGE = "STORAGE"
}

export function productAreaTitle(area: ProductArea): string {
    switch (area) {
        case ProductArea.COMPUTE:
            return "Compute";
        case ProductArea.STORAGE:
            return "Storage";
    }
}

export interface WalletBalance {
    wallet: Wallet;
    balance: number;
    allocated: number;
    used: number;
    area: ProductArea;
}

export interface Wallet {
    id: string;
    type: AccountType;
    paysFor: ProductCategoryId;
}

export function walletEquals(a: Wallet, b: Wallet): boolean {
    return a.id === b.id && a.type === b.type && productCategoryEquals(a.paysFor, b.paysFor);
}

export function productCategoryEquals(a: ProductCategoryId, b: ProductCategoryId): boolean {
    return a.provider === b.provider && a.id === b.id;
}

export interface RetrieveBalanceRequest {
    id?: string;
    type?: AccountType;
    includeChildren?: boolean;
}

export interface RetrieveBalanceResponse {
    wallets: WalletBalance[];
}

export function retrieveBalance(request: RetrieveBalanceRequest): APICallParameters<RetrieveBalanceRequest> {
    return {
        method: "GET",
        path: buildQueryString("/accounting/wallets/balance", request),
        parameters: request,
        reloadId: Math.random()
    };
}

export interface GrantCreditsRequest {
    wallet: Wallet;
    credits: number;
}

export type GrantCreditsResponse = {};

export function grantCredits(request: GrantCreditsRequest): APICallParameters<GrantCreditsRequest> {
    return {
        method: "POST",
        path: "/accounting/wallets/add-credits",
        parameters: request,
        payload: request,
        reloadId: Math.random()
    };
}

export interface SetCreditsRequest {
    wallet: Wallet;
    lastKnownBalance: number;
    newBalance: number;
}

export type SetCreditsResponse = {};

export function setCredits(request: SetCreditsRequest): APICallParameters<SetCreditsRequest> {
    return {
        method: "POST",
        path: "/accounting/wallets/set-balance",
        parameters: request,
        payload: request,
        reloadId: Math.random()
    };
}

export interface RetrieveCreditsRequest {
    id: string;
    type: AccountType;
}

export interface RetrieveCreditsResponse {
    wallets: WalletBalance[];
}

export function retrieveCredits(request: RetrieveCreditsRequest): APICallParameters<RetrieveCreditsRequest> {
    return {
        method: "GET",
        path: buildQueryString("/accounting/wallets/balance", request),
        parameters: request,
        reloadId: Math.random()
    };
}

// Machines

export interface Product {
    id: string;
    pricePerUnit: number;
    category: ProductCategoryId;
    description: string;
    availability: {type: "available" | "unavailable"; reason?: string};
    priority: number;
    cpu?: number;
    memoryInGigs?: number;
    gpu?: number;
    type: "compute" | "storage";
}

export interface ListProductsRequest extends PaginationRequest {
    provider: string;
}

export interface ListProductsByAreaRequest extends PaginationRequest {
    provider: string;
    area: string
}

export type ListProductsResponse = Product[];

export function listProducts(request: ListProductsRequest): APICallParameters<ListProductsRequest> {
    return {
        method: "GET",
        path: buildQueryString("/products/list", request),
        parameters: request,
        reloadId: Math.random()
    };
}

export function listByProductArea(request: ListProductsByAreaRequest): APICallParameters<ListProductsByAreaRequest> {
    return {
        method: "GET",
        path: buildQueryString("/products/listByArea", request),
        parameters: request,
        reloadId: Math.random()
    };
}

export interface RetrieveFromProviderRequest {provider: string;}
export type RetrieveFromProviderResponse = Product[];
export function retrieveFromProvider(
    request: RetrieveFromProviderRequest
): APICallParameters<RetrieveFromProviderRequest> {
    return {
        method: "GET",
        path: buildQueryString("/products/retrieve", request),
        parameters: request,
        reloadId: Math.random()
    };
}

export interface TimeRangeQuery {
    bucketSize: number;
    periodStart: number;
    periodEnd: number;
}

export interface UsageRequest extends TimeRangeQuery {
}

export interface UsagePoint {
    timestamp: number;
    creditsUsed: number;
}

export interface UsageLine {
    area: ProductArea;
    category: string;
    projectPath?: string;
    projectId?: string;
    points: UsagePoint[];
}

export interface UsageChart {
    provider: string;
    lines: UsageLine[];
}

export interface UsageResponse {
    charts: UsageChart[];
}

export function usage(request: UsageRequest): APICallParameters<UsageRequest> {
    return {
        method: "GET",
        path: buildQueryString("/accounting/visualization/usage", request),
        parameters: request,
        reloadId: Math.random()
    };
}

export interface NativeChartPoint extends Record<string, number> {
    time: number;
}

export interface UsageRow {
    title: string;
    projectId: string;
    projectPath: string;
    creditsUsed: number;
    creditsRemaining: number;
    wallet?: WalletBalance;
    children: UsageRow[];
}

export interface UsageTable {
    rows: UsageRow[];
}

interface AccountingProject {
    categories: CategoryUsage[];
    totalUsage: number;
    totalAllocated: number;
    projectId: string;
    projectTitle: string;
}

interface CategoryUsage {
    product: string;
    usage: number;
    allocated: number;
}

export function transformUsageChartForTable(
    chart: UsageChart,
    type: ProductArea,
    wallets: WalletBalance[],
    expanded: Set<string>
): {provider: string; projects: AccountingProject[]} {
    const projectMap: Record<string, AccountingProject> = {};
    const relevantWallets = wallets.filter(it => it.area === type && it.wallet.type === "PROJECT");

    chart.lines.filter(it => it.area === type).forEach(line => {
        const lineUsage = line.points.reduce((acc, p) => p.creditsUsed + acc, 0);
        const allocated = relevantWallets.find(it => it.wallet.id === line.projectId)?.allocated ?? 0;
        if (!projectMap[line.projectPath!]) {
            projectMap[line.projectPath!] = {
                categories: expanded.has(line.projectPath!) ?
                    [{product: line.category, usage: lineUsage, allocated}] : [],
                totalUsage: lineUsage,
                totalAllocated: relevantWallets.find(it => it.wallet.id === line.projectId)?.allocated ?? 0,
                projectId: line.projectId!,
                projectTitle: line.projectPath!
            };
        } else {
            const project = projectMap[line.projectPath!];
            if (expanded.has(line.projectPath!)) {
                projectMap[line.projectPath!].categories =
                    project.categories.concat([{product: line.category, usage: lineUsage, allocated}]);
            }
            projectMap[line.projectPath!].totalUsage += lineUsage;
            projectMap[line.projectPath!].totalAllocated += allocated;
        }
    });

    const projects: AccountingProject[] = [];

    for (const project of Object.keys(projectMap)) {
        projectMap[project].categories.sort((a, b) => b.usage - a.usage);
        projects.push(projectMap[project]);
    }

    return {provider: chart.provider, projects: projects.sort((a, b) => b.totalUsage - a.totalUsage)};
}

export interface NativeChart {
    provider: string;
    lineNames: string[];
    points: NativeChartPoint[];
}

export function transformUsageChartForCharting(
    chart: UsageChart,
    type: ProductArea
): NativeChart {
    const builder: Record<string, NativeChartPoint> = {};
    let lineNames: string[] = [];
    const numberToInclude = 4;
    const otherId = "Others";

    const usagePerProject: Record<string, number> = {};
    for (const line of chart.lines) {
        if (type !== line.area) continue;
        if (line.projectPath === undefined) continue;

        if (!lineNames.includes(line.projectPath)) {
            lineNames.push(line.projectPath);
        }

        usagePerProject[line.projectPath] =
            (usagePerProject[line.projectPath] ?? 0) +
            line.points.reduce((prev, cur) => prev + cur.creditsUsed, 0);
    }

    lineNames.sort((a, b) => (usagePerProject[b] ?? 0) - (usagePerProject[a] ?? 0));
    lineNames = lineNames.filter((ignored, idx) => idx < numberToInclude);
    lineNames.push(otherId);

    for (const line of chart.lines) {
        if (type !== line.area) continue;
        if (!line.projectPath) continue;

        const ranking = lineNames.indexOf(line.projectPath);
        let id = line.projectPath;
        if (ranking === -1) {
            id = otherId;
        }

        for (const point of line.points) {
            const dataPoint = builder[`${point.timestamp}`] ?? {time: point.timestamp};

            if (dataPoint[id] === undefined) dataPoint[id] = 0;
            dataPoint[id] += point.creditsUsed;

            builder[`${point.timestamp}`] = dataPoint;
        }
    }

    return {provider: chart.provider, lineNames, points: Object.values(builder)};
}


export interface RetrieveQuotaRequest {
    path: string;
    includeUsage?: boolean;
}

export interface RetrieveQuotaResponse {
    quotaInBytes: number;
    quotaInTotal: number;
    quotaUsed?: number;
}

export function retrieveQuota(request: RetrieveQuotaRequest): APICallParameters<RetrieveQuotaRequest> {
    return {
        method: "GET",
        path: buildQueryString("/files/quota", request),
        parameters: request,
        reloadId: Math.random()
    };
}

export interface UpdateQuotaRequest {
    path: string;
    quotaInBytes: number;
}

export type UpdateQuotaResponse = {};

export function updateQuota(request: UpdateQuotaRequest): APICallParameters<UpdateQuotaRequest> {
    return {
        method: "POST",
        path: "/files/quota",
        parameters: request,
        payload: request,
        reloadId: Math.random()
    };
}

export const UCLOUD_PROVIDER = "ucloud";

export function isQuotaSupported(category: ProductCategoryId): boolean {
    return category.provider === UCLOUD_PROVIDER && category.id === "u1-cephfs";
}
