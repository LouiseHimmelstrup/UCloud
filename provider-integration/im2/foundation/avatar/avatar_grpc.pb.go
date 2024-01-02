// Code generated by protoc-gen-go-grpc. DO NOT EDIT.
// versions:
// - protoc-gen-go-grpc v1.2.0
// - protoc             v4.25.1
// source: avatar.proto

package avatar

import (
	context "context"
	grpc "google.golang.org/grpc"
	codes "google.golang.org/grpc/codes"
	status "google.golang.org/grpc/status"
)

// This is a compile-time assertion to ensure that this generated file
// is compatible with the grpc package it is being compiled against.
// Requires gRPC-Go v1.32.0 or later.
const _ = grpc.SupportPackageIsVersion7

// AvatarServiceClient is the client API for AvatarService service.
//
// For semantics around ctx use and closing/ending streaming RPCs, please refer to https://pkg.go.dev/google.golang.org/grpc/?tab=doc#ClientConn.NewStream.
type AvatarServiceClient interface {
	RetrieveMyAvatar(ctx context.Context, in *Empty, opts ...grpc.CallOption) (*Avatar, error)
	Update(ctx context.Context, in *Avatar, opts ...grpc.CallOption) (*Empty, error)
	FindAvatars(ctx context.Context, in *FindAvatarsRequest, opts ...grpc.CallOption) (*FindAvatarsReply, error)
}

type avatarServiceClient struct {
	cc grpc.ClientConnInterface
}

func NewAvatarServiceClient(cc grpc.ClientConnInterface) AvatarServiceClient {
	return &avatarServiceClient{cc}
}

func (c *avatarServiceClient) RetrieveMyAvatar(ctx context.Context, in *Empty, opts ...grpc.CallOption) (*Avatar, error) {
	out := new(Avatar)
	err := c.cc.Invoke(ctx, "/avatar.AvatarService/RetrieveMyAvatar", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *avatarServiceClient) Update(ctx context.Context, in *Avatar, opts ...grpc.CallOption) (*Empty, error) {
	out := new(Empty)
	err := c.cc.Invoke(ctx, "/avatar.AvatarService/Update", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *avatarServiceClient) FindAvatars(ctx context.Context, in *FindAvatarsRequest, opts ...grpc.CallOption) (*FindAvatarsReply, error) {
	out := new(FindAvatarsReply)
	err := c.cc.Invoke(ctx, "/avatar.AvatarService/FindAvatars", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

// AvatarServiceServer is the server API for AvatarService service.
// All implementations must embed UnimplementedAvatarServiceServer
// for forward compatibility
type AvatarServiceServer interface {
	RetrieveMyAvatar(context.Context, *Empty) (*Avatar, error)
	Update(context.Context, *Avatar) (*Empty, error)
	FindAvatars(context.Context, *FindAvatarsRequest) (*FindAvatarsReply, error)
	mustEmbedUnimplementedAvatarServiceServer()
}

// UnimplementedAvatarServiceServer must be embedded to have forward compatible implementations.
type UnimplementedAvatarServiceServer struct {
}

func (UnimplementedAvatarServiceServer) RetrieveMyAvatar(context.Context, *Empty) (*Avatar, error) {
	return nil, status.Errorf(codes.Unimplemented, "method RetrieveMyAvatar not implemented")
}
func (UnimplementedAvatarServiceServer) Update(context.Context, *Avatar) (*Empty, error) {
	return nil, status.Errorf(codes.Unimplemented, "method Update not implemented")
}
func (UnimplementedAvatarServiceServer) FindAvatars(context.Context, *FindAvatarsRequest) (*FindAvatarsReply, error) {
	return nil, status.Errorf(codes.Unimplemented, "method FindAvatars not implemented")
}
func (UnimplementedAvatarServiceServer) mustEmbedUnimplementedAvatarServiceServer() {}

// UnsafeAvatarServiceServer may be embedded to opt out of forward compatibility for this service.
// Use of this interface is not recommended, as added methods to AvatarServiceServer will
// result in compilation errors.
type UnsafeAvatarServiceServer interface {
	mustEmbedUnimplementedAvatarServiceServer()
}

func RegisterAvatarServiceServer(s grpc.ServiceRegistrar, srv AvatarServiceServer) {
	s.RegisterService(&AvatarService_ServiceDesc, srv)
}

func _AvatarService_RetrieveMyAvatar_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(Empty)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(AvatarServiceServer).RetrieveMyAvatar(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/avatar.AvatarService/RetrieveMyAvatar",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(AvatarServiceServer).RetrieveMyAvatar(ctx, req.(*Empty))
	}
	return interceptor(ctx, in, info, handler)
}

func _AvatarService_Update_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(Avatar)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(AvatarServiceServer).Update(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/avatar.AvatarService/Update",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(AvatarServiceServer).Update(ctx, req.(*Avatar))
	}
	return interceptor(ctx, in, info, handler)
}

func _AvatarService_FindAvatars_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(FindAvatarsRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(AvatarServiceServer).FindAvatars(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/avatar.AvatarService/FindAvatars",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(AvatarServiceServer).FindAvatars(ctx, req.(*FindAvatarsRequest))
	}
	return interceptor(ctx, in, info, handler)
}

// AvatarService_ServiceDesc is the grpc.ServiceDesc for AvatarService service.
// It's only intended for direct use with grpc.RegisterService,
// and not to be introspected or modified (even as a copy)
var AvatarService_ServiceDesc = grpc.ServiceDesc{
	ServiceName: "avatar.AvatarService",
	HandlerType: (*AvatarServiceServer)(nil),
	Methods: []grpc.MethodDesc{
		{
			MethodName: "RetrieveMyAvatar",
			Handler:    _AvatarService_RetrieveMyAvatar_Handler,
		},
		{
			MethodName: "Update",
			Handler:    _AvatarService_Update_Handler,
		},
		{
			MethodName: "FindAvatars",
			Handler:    _AvatarService_FindAvatars_Handler,
		},
	},
	Streams:  []grpc.StreamDesc{},
	Metadata: "avatar.proto",
}
