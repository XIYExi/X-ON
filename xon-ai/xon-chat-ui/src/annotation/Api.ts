
import request from '@/utils/request'
export type RequestPageType<T> = T & {
    pageNum: number,
    pageSize: number,
    params: { [key: string]: string }
}
export type ResponseResultType<T> = {
    code: number
    data: T
    msg: string
}
export type ResponseTableType<T> = {
    code: number
    msg: string
    rows: T[]
    total: number
}
export type ApiServiceType<T> = {
    list: (query: RequestPageType<T>) => Promise<ResponseTableType<T>>
    get: (id: number | string) => Promise<ResponseResultType<T>>
    add: (data: T) => Promise<ResponseResultType<T>>
    update: (data: T) => Promise<ResponseResultType<T>>
    del: (id: number | string) => Promise<ResponseResultType<undefined>>
}

export function Page<T>(target: new (...args: any[]) => T): RequestPageType<T> {
    let o = new target();
    return {
        pageNum: 1,
        pageSize: 10,
        params: {},
        ...o
    };
}

export function ApiService<T>(target: new (...args: any[]) => T): ApiServiceType<T> {
    return target.prototype.api
}

export default function Api(url: string) {
    return function <T>(target: any) {
        target.prototype.api = {}
        target.prototype.api = {
            list: (query: RequestPageType<T>) => request({ url: url + '/list', method: 'get', params: query }),
            get: (id: number | string) => request({ url: url + '/' + id, method: 'get' }),
            add: (data: T) => request({ url, method: 'post', data }),
            update: (data: T) => request({ url, method: 'put', data }),
            del: (id: number | string) => request({ url: url + '/' + id, method: 'delete' }),
        }

    }
}
