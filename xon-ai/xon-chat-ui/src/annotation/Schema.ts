import { Component } from "vue";

export default function Schema(name: string): (target: any, attr: any) => void;
export default function Schema(options: SchemaTyle): (target: any, attr: any) => void;
export default function Schema(value: SchemaTyle | string) {
    return function (target: any, attr: any) {
        if (target.schema == undefined) target.schema = {}
        if (typeof value === 'string') target.schema[attr] = { name: value, components: {}, attr };
        else {
            target.schema[attr] = value;
        }
    }
}
export interface SchemaTyle {
    name: string,
    attr:string,
    components: { [key: string]: (...args: any[]) => Component }
}
export function getSchema(target: any, prop: string): SchemaTyle {
    const constructor = typeof target === 'function' ? target : target.constructor
    const schemaObj = constructor.prototype.schema
    if (!!schemaObj) {
        const schema: SchemaTyle = schemaObj[prop]
        if (!!schema) return schema
        else throw new Error(`${constructor.name}'s ${prop} have not @schema`)
    } else {
        throw new Error(`${constructor.name} have not @schema`)
    }
}

export function getSchemas(target: any): SchemaTyle[] {
    const constructor = typeof target === 'function' ? target : target.constructor
    const schemaObj: { [key: string]: SchemaTyle } = constructor.prototype.schema
    if (!!schemaObj) {
        return Object.values(schemaObj)
    } else {
        throw new Error(`${constructor.name} have not @schema`)
    }
}

export function getSchemaName(target: any, prop: string): string {
    const schema = getSchema(target, prop)
    return schema.name
}


