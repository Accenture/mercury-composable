import { describe, expect, it } from 'vitest';
import { parseCreateNodeTextResult } from '../messageParser';

describe('parseCreateNodeTextResult', () => {
  it('parses accepted create-node text', () => {
    expect(parseCreateNodeTextResult('node root created')).toEqual({
      status: 'accepted',
      alias: 'root',
      message: 'node root created',
    });
  });

  it('parses duplicate create-node text as rejected', () => {
    expect(parseCreateNodeTextResult('node root already exists')).toEqual({
      status: 'rejected',
      alias: 'root',
      message: 'node root already exists',
    });
  });

  it('parses generic backend errors without alias', () => {
    expect(parseCreateNodeTextResult('ERROR: alias is reserved')).toEqual({
      status: 'error',
      alias: null,
      message: 'ERROR: alias is reserved',
    });
  });

  it('ignores command echoes and unrelated node operations', () => {
    expect(parseCreateNodeTextResult('> create node root')).toBeNull();
    expect(parseCreateNodeTextResult('node root updated')).toBeNull();
    expect(parseCreateNodeTextResult('{"type":"info"}')).toBeNull();
  });
});
