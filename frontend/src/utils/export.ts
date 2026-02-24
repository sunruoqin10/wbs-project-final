import * as XLSX from 'xlsx';

export interface ExportColumn {
  key: string;
  label: string;
  width?: number;
}

export function exportToExcel<T>(
  data: T[],
  columns: ExportColumn[],
  filename: string
): void {
  const worksheetData: any[][] = [];

  worksheetData.push(columns.map(col => col.label));

  data.forEach(row => {
    const rowData: any[] = [];
    columns.forEach(col => {
      rowData.push((row as any)[col.key] || '');
    });
    worksheetData.push(rowData);
  });

  const worksheet = XLSX.utils.aoa_to_sheet(worksheetData);

  columns.forEach((col, index) => {
    const colChar = String.fromCharCode(65 + index);
    worksheet['!cols'] = worksheet['!cols'] || [];
    worksheet['!cols'][index] = {
      wch: col.width || 20
    };
  });

  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, worksheet, 'Sheet1');

  const date = new Date();
  const dateStr = `${date.getFullYear()}${String(date.getMonth() + 1).padStart(2, '0')}${String(date.getDate()).padStart(2, '0')}`;
  const finalFilename = `${filename}_${dateStr}.xlsx`;

  XLSX.writeFile(workbook, finalFilename);
}
