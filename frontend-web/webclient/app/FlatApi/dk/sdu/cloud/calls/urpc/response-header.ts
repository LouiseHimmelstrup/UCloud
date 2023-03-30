// automatically generated by the FlatBuffers compiler, do not modify

import * as flatbuffers from 'flatbuffers';

export class ResponseHeader {
  bb: flatbuffers.ByteBuffer|null = null;
  bb_pos = 0;
  __init(i:number, bb:flatbuffers.ByteBuffer):ResponseHeader {
  this.bb_pos = i;
  this.bb = bb;
  return this;
}

static getRootAsResponseHeader(bb:flatbuffers.ByteBuffer, obj?:ResponseHeader):ResponseHeader {
  return (obj || new ResponseHeader()).__init(bb.readInt32(bb.position()) + bb.position(), bb);
}

static getSizePrefixedRootAsResponseHeader(bb:flatbuffers.ByteBuffer, obj?:ResponseHeader):ResponseHeader {
  bb.setPosition(bb.position() + flatbuffers.SIZE_PREFIX_LENGTH);
  return (obj || new ResponseHeader()).__init(bb.readInt32(bb.position()) + bb.position(), bb);
}

status():number {
  const offset = this.bb!.__offset(this.bb_pos, 4);
  return offset ? this.bb!.readUint8(this.bb_pos + offset) : 0;
}

stream():number {
  const offset = this.bb!.__offset(this.bb_pos, 6);
  return offset ? this.bb!.readUint16(this.bb_pos + offset) : 0;
}

endOfStream():boolean {
  const offset = this.bb!.__offset(this.bb_pos, 8);
  return offset ? !!this.bb!.readInt8(this.bb_pos + offset) : false;
}

static startResponseHeader(builder:flatbuffers.Builder) {
  builder.startObject(3);
}

static addStatus(builder:flatbuffers.Builder, status:number) {
  builder.addFieldInt8(0, status, 0);
}

static addStream(builder:flatbuffers.Builder, stream:number) {
  builder.addFieldInt16(1, stream, 0);
}

static addEndOfStream(builder:flatbuffers.Builder, endOfStream:boolean) {
  builder.addFieldInt8(2, +endOfStream, +false);
}

static endResponseHeader(builder:flatbuffers.Builder):flatbuffers.Offset {
  const offset = builder.endObject();
  return offset;
}

static createResponseHeader(builder:flatbuffers.Builder, status:number, stream:number, endOfStream:boolean):flatbuffers.Offset {
  ResponseHeader.startResponseHeader(builder);
  ResponseHeader.addStatus(builder, status);
  ResponseHeader.addStream(builder, stream);
  ResponseHeader.addEndOfStream(builder, endOfStream);
  return ResponseHeader.endResponseHeader(builder);
}
}
