/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue';
  const component: DefineComponent<{}, {}, any>;
  export default component;
}

declare module 'dhtmlx-gantt' {
  export const gantt: any;
}

declare module 'dayjs/locale/*' {
  const locale: any;
  export default locale;
}

declare module 'dayjs/plugin/*' {
  const plugin: any;
  export default plugin;
}

declare module 'jspdf' {
  interface jsPDF {
    text(text: string, x: number, y: number): jsPDF;
    save(filename: string): void;
  }
  const jsPDF: {
    new (options?: { orientation?: string; unit?: string; format?: string | number[] }): jsPDF;
  };
  export = jsPDF;
}

declare module 'jspdf-autotable' {
  import { jsPDF } from 'jspdf';
  interface UserOptions {
    head?: string[][];
    body?: string[][];
    startY?: number;
    styles?: any;
    headStyles?: any;
    bodyStyles?: any;
    alternateRowStyles?: any;
    columnStyles?: any;
    didDrawPage?: (data: any) => void;
  }
  export function autoTable(doc: jsPDF, options: UserOptions): void;
}

declare module 'xlsx' {
  export const utils: {
    json_to_sheet(data: any[]): any;
    book_new(): any;
    book_append_sheet(wb: any, ws: any, name: string): void;
  };
  export function writeFile(wb: any, filename: string): void;
}

declare module 'vuedraggable' {
  import { DefineComponent } from 'vue';
  const draggable: DefineComponent;
  export default draggable;
}
