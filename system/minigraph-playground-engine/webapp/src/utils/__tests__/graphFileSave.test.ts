// @vitest-environment happy-dom

import { afterEach, describe, expect, it, vi } from 'vitest';
import { saveBinaryFile, saveTextFile, supportsSaveFilePicker } from '../graphFile';

type PickerWindow = Window & { showSaveFilePicker?: unknown };

afterEach(() => {
  delete (window as PickerWindow).showSaveFilePicker;
  vi.restoreAllMocks();
});

describe('saveBinaryFile', () => {
  it('writes the bytes through the native dialog as a .pack and answers the chosen name', async () => {
    const write = vi.fn().mockResolvedValue(undefined);
    const close = vi.fn().mockResolvedValue(undefined);
    const picker = vi.fn().mockResolvedValue({ name: 'renamed.pack', createWritable: async () => ({ write, close }) });
    (window as PickerWindow).showSaveFilePicker = picker;
    expect(supportsSaveFilePicker()).toBe(true);

    const bytes = new Uint8Array([0x82, 0xa8, 0x6d, 0x61]);
    const outcome = await saveBinaryFile(bytes, 'demo.pack');

    expect(outcome).toEqual({ saved: true, fileName: 'renamed.pack' });
    expect(picker).toHaveBeenCalledWith({
      suggestedName: 'demo.pack',
      excludeAcceptAllOption: true,
      types: [{ description: 'MiniGraph graph set', accept: { 'application/octet-stream': ['.pack'] } }],
    });
    expect(write).toHaveBeenCalledWith(bytes);
    expect(close).toHaveBeenCalledTimes(1);
  });

  it('reports a cancelled dialog without throwing', async () => {
    (window as PickerWindow).showSaveFilePicker = vi.fn().mockRejectedValue(Object.assign(new Error('cancelled'), { name: 'AbortError' }));
    expect(await saveBinaryFile(new Uint8Array([1]), 'demo.pack')).toEqual({ saved: false, cancelled: true });
  });

  it('falls back to a download of an octet-stream blob under the file name', async () => {
    expect(supportsSaveFilePicker()).toBe(false);
    const blobs: Blob[] = [];
    vi.spyOn(URL, 'createObjectURL').mockImplementation((blob: Blob | MediaSource) => {
      blobs.push(blob as Blob);
      return 'blob:demo';
    });
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    const clicked: HTMLAnchorElement[] = [];
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) {
      clicked.push(this);
    });

    const outcome = await saveBinaryFile(new Uint8Array([1, 2, 3]), 'demo.pack');

    expect(outcome).toEqual({ saved: true, fileName: 'demo.pack' });
    expect(blobs).toHaveLength(1);
    expect(blobs[0].type).toBe('application/octet-stream');
    expect(blobs[0].size).toBe(3);
    expect(clicked).toHaveLength(1);
    expect(clicked[0].download).toBe('demo.pack');
    expect(clicked[0].href).toContain('blob:demo');
  });
});

describe('saveTextFile', () => {
  it('still offers the JSON type through the native dialog', async () => {
    const write = vi.fn().mockResolvedValue(undefined);
    const picker = vi.fn().mockResolvedValue({ name: 'demo.json', createWritable: async () => ({ write, close: vi.fn() }) });
    (window as PickerWindow).showSaveFilePicker = picker;

    await saveTextFile('{}', 'demo.json');

    expect(picker.mock.calls[0][0].types).toEqual([{ description: 'MiniGraph model (JSON)', accept: { 'application/json': ['.json'] } }]);
    expect(write).toHaveBeenCalledWith('{}');
  });
});
