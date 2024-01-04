// Code generated by protoc-gen-go. DO NOT EDIT.
// versions:
// 	protoc-gen-go v1.28.1
// 	protoc        v4.25.1
// source: resources.proto

package resources

import (
	protoreflect "google.golang.org/protobuf/reflect/protoreflect"
	protoimpl "google.golang.org/protobuf/runtime/protoimpl"
	reflect "reflect"
	sync "sync"
)

const (
	// Verify that this generated code is sufficiently up-to-date.
	_ = protoimpl.EnforceVersion(20 - protoimpl.MinVersion)
	// Verify that runtime/protoimpl is sufficiently up-to-date.
	_ = protoimpl.EnforceVersion(protoimpl.MaxVersion - 20)
)

type Permission int32

const (
	Permission_PERMISSION_UNSPECIFIED Permission = 0
	Permission_PERMISSION_READ        Permission = 1
	Permission_PERMISSION_EDIT        Permission = 2
	Permission_PERMISSION_ADMIN       Permission = 3
	Permission_PERMISSION_PROVIDER    Permission = 4
)

// Enum value maps for Permission.
var (
	Permission_name = map[int32]string{
		0: "PERMISSION_UNSPECIFIED",
		1: "PERMISSION_READ",
		2: "PERMISSION_EDIT",
		3: "PERMISSION_ADMIN",
		4: "PERMISSION_PROVIDER",
	}
	Permission_value = map[string]int32{
		"PERMISSION_UNSPECIFIED": 0,
		"PERMISSION_READ":        1,
		"PERMISSION_EDIT":        2,
		"PERMISSION_ADMIN":       3,
		"PERMISSION_PROVIDER":    4,
	}
)

func (x Permission) Enum() *Permission {
	p := new(Permission)
	*p = x
	return p
}

func (x Permission) String() string {
	return protoimpl.X.EnumStringOf(x.Descriptor(), protoreflect.EnumNumber(x))
}

func (Permission) Descriptor() protoreflect.EnumDescriptor {
	return file_resources_proto_enumTypes[0].Descriptor()
}

func (Permission) Type() protoreflect.EnumType {
	return &file_resources_proto_enumTypes[0]
}

func (x Permission) Number() protoreflect.EnumNumber {
	return protoreflect.EnumNumber(x)
}

// Deprecated: Use Permission.Descriptor instead.
func (Permission) EnumDescriptor() ([]byte, []int) {
	return file_resources_proto_rawDescGZIP(), []int{0}
}

type AclEntry_EntityType int32

const (
	AclEntry_ENTITY_TYPE_UNSPECIFIED   AclEntry_EntityType = 0
	AclEntry_ENTITY_TYPE_USER          AclEntry_EntityType = 1
	AclEntry_ENTITY_TYPE_PROJECT_GROUP AclEntry_EntityType = 2
)

// Enum value maps for AclEntry_EntityType.
var (
	AclEntry_EntityType_name = map[int32]string{
		0: "ENTITY_TYPE_UNSPECIFIED",
		1: "ENTITY_TYPE_USER",
		2: "ENTITY_TYPE_PROJECT_GROUP",
	}
	AclEntry_EntityType_value = map[string]int32{
		"ENTITY_TYPE_UNSPECIFIED":   0,
		"ENTITY_TYPE_USER":          1,
		"ENTITY_TYPE_PROJECT_GROUP": 2,
	}
)

func (x AclEntry_EntityType) Enum() *AclEntry_EntityType {
	p := new(AclEntry_EntityType)
	*p = x
	return p
}

func (x AclEntry_EntityType) String() string {
	return protoimpl.X.EnumStringOf(x.Descriptor(), protoreflect.EnumNumber(x))
}

func (AclEntry_EntityType) Descriptor() protoreflect.EnumDescriptor {
	return file_resources_proto_enumTypes[1].Descriptor()
}

func (AclEntry_EntityType) Type() protoreflect.EnumType {
	return &file_resources_proto_enumTypes[1]
}

func (x AclEntry_EntityType) Number() protoreflect.EnumNumber {
	return protoreflect.EnumNumber(x)
}

// Deprecated: Use AclEntry_EntityType.Descriptor instead.
func (AclEntry_EntityType) EnumDescriptor() ([]byte, []int) {
	return file_resources_proto_rawDescGZIP(), []int{3, 0}
}

type Metadata struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Id          int64        `protobuf:"varint,1,opt,name=id,proto3" json:"id,omitempty"`
	CreatedAt   int64        `protobuf:"varint,2,opt,name=created_at,json=createdAt,proto3" json:"created_at,omitempty"`
	Owner       *Owner       `protobuf:"bytes,3,opt,name=owner,proto3" json:"owner,omitempty"`
	Permissions *Permissions `protobuf:"bytes,4,opt,name=permissions,proto3" json:"permissions,omitempty"`
}

func (x *Metadata) Reset() {
	*x = Metadata{}
	if protoimpl.UnsafeEnabled {
		mi := &file_resources_proto_msgTypes[0]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *Metadata) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*Metadata) ProtoMessage() {}

func (x *Metadata) ProtoReflect() protoreflect.Message {
	mi := &file_resources_proto_msgTypes[0]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use Metadata.ProtoReflect.Descriptor instead.
func (*Metadata) Descriptor() ([]byte, []int) {
	return file_resources_proto_rawDescGZIP(), []int{0}
}

func (x *Metadata) GetId() int64 {
	if x != nil {
		return x.Id
	}
	return 0
}

func (x *Metadata) GetCreatedAt() int64 {
	if x != nil {
		return x.CreatedAt
	}
	return 0
}

func (x *Metadata) GetOwner() *Owner {
	if x != nil {
		return x.Owner
	}
	return nil
}

func (x *Metadata) GetPermissions() *Permissions {
	if x != nil {
		return x.Permissions
	}
	return nil
}

type Owner struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	CreatedBy string `protobuf:"bytes,1,opt,name=created_by,json=createdBy,proto3" json:"created_by,omitempty"`
	Project   string `protobuf:"bytes,2,opt,name=project,proto3" json:"project,omitempty"`
}

func (x *Owner) Reset() {
	*x = Owner{}
	if protoimpl.UnsafeEnabled {
		mi := &file_resources_proto_msgTypes[1]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *Owner) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*Owner) ProtoMessage() {}

func (x *Owner) ProtoReflect() protoreflect.Message {
	mi := &file_resources_proto_msgTypes[1]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use Owner.ProtoReflect.Descriptor instead.
func (*Owner) Descriptor() ([]byte, []int) {
	return file_resources_proto_rawDescGZIP(), []int{1}
}

func (x *Owner) GetCreatedBy() string {
	if x != nil {
		return x.CreatedBy
	}
	return ""
}

func (x *Owner) GetProject() string {
	if x != nil {
		return x.Project
	}
	return ""
}

type Permissions struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	Myself []Permission `protobuf:"varint,1,rep,packed,name=myself,proto3,enum=resources.Permission" json:"myself,omitempty"`
	Others []*AclEntry  `protobuf:"bytes,2,rep,name=others,proto3" json:"others,omitempty"`
}

func (x *Permissions) Reset() {
	*x = Permissions{}
	if protoimpl.UnsafeEnabled {
		mi := &file_resources_proto_msgTypes[2]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *Permissions) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*Permissions) ProtoMessage() {}

func (x *Permissions) ProtoReflect() protoreflect.Message {
	mi := &file_resources_proto_msgTypes[2]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use Permissions.ProtoReflect.Descriptor instead.
func (*Permissions) Descriptor() ([]byte, []int) {
	return file_resources_proto_rawDescGZIP(), []int{2}
}

func (x *Permissions) GetMyself() []Permission {
	if x != nil {
		return x.Myself
	}
	return nil
}

func (x *Permissions) GetOthers() []*AclEntry {
	if x != nil {
		return x.Others
	}
	return nil
}

type AclEntry struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	EntityId      string              `protobuf:"bytes,1,opt,name=entity_id,json=entityId,proto3" json:"entity_id,omitempty"`
	EntityProject string              `protobuf:"bytes,2,opt,name=entity_project,json=entityProject,proto3" json:"entity_project,omitempty"`
	EntityType    AclEntry_EntityType `protobuf:"varint,3,opt,name=entity_type,json=entityType,proto3,enum=resources.AclEntry_EntityType" json:"entity_type,omitempty"`
	Permissions   []Permission        `protobuf:"varint,4,rep,packed,name=permissions,proto3,enum=resources.Permission" json:"permissions,omitempty"`
}

func (x *AclEntry) Reset() {
	*x = AclEntry{}
	if protoimpl.UnsafeEnabled {
		mi := &file_resources_proto_msgTypes[3]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *AclEntry) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*AclEntry) ProtoMessage() {}

func (x *AclEntry) ProtoReflect() protoreflect.Message {
	mi := &file_resources_proto_msgTypes[3]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use AclEntry.ProtoReflect.Descriptor instead.
func (*AclEntry) Descriptor() ([]byte, []int) {
	return file_resources_proto_rawDescGZIP(), []int{3}
}

func (x *AclEntry) GetEntityId() string {
	if x != nil {
		return x.EntityId
	}
	return ""
}

func (x *AclEntry) GetEntityProject() string {
	if x != nil {
		return x.EntityProject
	}
	return ""
}

func (x *AclEntry) GetEntityType() AclEntry_EntityType {
	if x != nil {
		return x.EntityType
	}
	return AclEntry_ENTITY_TYPE_UNSPECIFIED
}

func (x *AclEntry) GetPermissions() []Permission {
	if x != nil {
		return x.Permissions
	}
	return nil
}

type UpdatedAcl struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	NewEntries     []*AclEntry `protobuf:"bytes,1,rep,name=new_entries,json=newEntries,proto3" json:"new_entries,omitempty"`
	DeletedEntries []*AclEntry `protobuf:"bytes,2,rep,name=deleted_entries,json=deletedEntries,proto3" json:"deleted_entries,omitempty"`
}

func (x *UpdatedAcl) Reset() {
	*x = UpdatedAcl{}
	if protoimpl.UnsafeEnabled {
		mi := &file_resources_proto_msgTypes[4]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *UpdatedAcl) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*UpdatedAcl) ProtoMessage() {}

func (x *UpdatedAcl) ProtoReflect() protoreflect.Message {
	mi := &file_resources_proto_msgTypes[4]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use UpdatedAcl.ProtoReflect.Descriptor instead.
func (*UpdatedAcl) Descriptor() ([]byte, []int) {
	return file_resources_proto_rawDescGZIP(), []int{4}
}

func (x *UpdatedAcl) GetNewEntries() []*AclEntry {
	if x != nil {
		return x.NewEntries
	}
	return nil
}

func (x *UpdatedAcl) GetDeletedEntries() []*AclEntry {
	if x != nil {
		return x.DeletedEntries
	}
	return nil
}

type Flags struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	IncludeOthers         bool    `protobuf:"varint,1,opt,name=include_others,json=includeOthers,proto3" json:"include_others,omitempty"`
	IncludeUpdates        bool    `protobuf:"varint,2,opt,name=include_updates,json=includeUpdates,proto3" json:"include_updates,omitempty"`
	IncludeSupport        bool    `protobuf:"varint,3,opt,name=include_support,json=includeSupport,proto3" json:"include_support,omitempty"`
	FilterCreatedBy       string  `protobuf:"bytes,4,opt,name=filter_created_by,json=filterCreatedBy,proto3" json:"filter_created_by,omitempty"`
	FilterCreatedAfter    int64   `protobuf:"varint,5,opt,name=filter_created_after,json=filterCreatedAfter,proto3" json:"filter_created_after,omitempty"`
	FilterCreatedBefore   int64   `protobuf:"varint,6,opt,name=filter_created_before,json=filterCreatedBefore,proto3" json:"filter_created_before,omitempty"`
	FilterProductId       string  `protobuf:"bytes,8,opt,name=filter_product_id,json=filterProductId,proto3" json:"filter_product_id,omitempty"`
	FilterProductCategory string  `protobuf:"bytes,9,opt,name=filter_product_category,json=filterProductCategory,proto3" json:"filter_product_category,omitempty"`
	FilterProductProvider string  `protobuf:"bytes,10,opt,name=filter_product_provider,json=filterProductProvider,proto3" json:"filter_product_provider,omitempty"`
	FilterIds             []int64 `protobuf:"varint,11,rep,packed,name=filter_ids,json=filterIds,proto3" json:"filter_ids,omitempty"`
	HideProductId         string  `protobuf:"bytes,12,opt,name=hide_product_id,json=hideProductId,proto3" json:"hide_product_id,omitempty"`
	HideProductCategory   string  `protobuf:"bytes,13,opt,name=hide_product_category,json=hideProductCategory,proto3" json:"hide_product_category,omitempty"`
	HideProductProvider   string  `protobuf:"bytes,14,opt,name=hide_product_provider,json=hideProductProvider,proto3" json:"hide_product_provider,omitempty"`
}

func (x *Flags) Reset() {
	*x = Flags{}
	if protoimpl.UnsafeEnabled {
		mi := &file_resources_proto_msgTypes[5]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *Flags) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*Flags) ProtoMessage() {}

func (x *Flags) ProtoReflect() protoreflect.Message {
	mi := &file_resources_proto_msgTypes[5]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use Flags.ProtoReflect.Descriptor instead.
func (*Flags) Descriptor() ([]byte, []int) {
	return file_resources_proto_rawDescGZIP(), []int{5}
}

func (x *Flags) GetIncludeOthers() bool {
	if x != nil {
		return x.IncludeOthers
	}
	return false
}

func (x *Flags) GetIncludeUpdates() bool {
	if x != nil {
		return x.IncludeUpdates
	}
	return false
}

func (x *Flags) GetIncludeSupport() bool {
	if x != nil {
		return x.IncludeSupport
	}
	return false
}

func (x *Flags) GetFilterCreatedBy() string {
	if x != nil {
		return x.FilterCreatedBy
	}
	return ""
}

func (x *Flags) GetFilterCreatedAfter() int64 {
	if x != nil {
		return x.FilterCreatedAfter
	}
	return 0
}

func (x *Flags) GetFilterCreatedBefore() int64 {
	if x != nil {
		return x.FilterCreatedBefore
	}
	return 0
}

func (x *Flags) GetFilterProductId() string {
	if x != nil {
		return x.FilterProductId
	}
	return ""
}

func (x *Flags) GetFilterProductCategory() string {
	if x != nil {
		return x.FilterProductCategory
	}
	return ""
}

func (x *Flags) GetFilterProductProvider() string {
	if x != nil {
		return x.FilterProductProvider
	}
	return ""
}

func (x *Flags) GetFilterIds() []int64 {
	if x != nil {
		return x.FilterIds
	}
	return nil
}

func (x *Flags) GetHideProductId() string {
	if x != nil {
		return x.HideProductId
	}
	return ""
}

func (x *Flags) GetHideProductCategory() string {
	if x != nil {
		return x.HideProductCategory
	}
	return ""
}

func (x *Flags) GetHideProductProvider() string {
	if x != nil {
		return x.HideProductProvider
	}
	return ""
}

var File_resources_proto protoreflect.FileDescriptor

var file_resources_proto_rawDesc = []byte{
	0x0a, 0x0f, 0x72, 0x65, 0x73, 0x6f, 0x75, 0x72, 0x63, 0x65, 0x73, 0x2e, 0x70, 0x72, 0x6f, 0x74,
	0x6f, 0x12, 0x09, 0x72, 0x65, 0x73, 0x6f, 0x75, 0x72, 0x63, 0x65, 0x73, 0x22, 0x9b, 0x01, 0x0a,
	0x08, 0x4d, 0x65, 0x74, 0x61, 0x64, 0x61, 0x74, 0x61, 0x12, 0x0e, 0x0a, 0x02, 0x69, 0x64, 0x18,
	0x01, 0x20, 0x01, 0x28, 0x03, 0x52, 0x02, 0x69, 0x64, 0x12, 0x1d, 0x0a, 0x0a, 0x63, 0x72, 0x65,
	0x61, 0x74, 0x65, 0x64, 0x5f, 0x61, 0x74, 0x18, 0x02, 0x20, 0x01, 0x28, 0x03, 0x52, 0x09, 0x63,
	0x72, 0x65, 0x61, 0x74, 0x65, 0x64, 0x41, 0x74, 0x12, 0x26, 0x0a, 0x05, 0x6f, 0x77, 0x6e, 0x65,
	0x72, 0x18, 0x03, 0x20, 0x01, 0x28, 0x0b, 0x32, 0x10, 0x2e, 0x72, 0x65, 0x73, 0x6f, 0x75, 0x72,
	0x63, 0x65, 0x73, 0x2e, 0x4f, 0x77, 0x6e, 0x65, 0x72, 0x52, 0x05, 0x6f, 0x77, 0x6e, 0x65, 0x72,
	0x12, 0x38, 0x0a, 0x0b, 0x70, 0x65, 0x72, 0x6d, 0x69, 0x73, 0x73, 0x69, 0x6f, 0x6e, 0x73, 0x18,
	0x04, 0x20, 0x01, 0x28, 0x0b, 0x32, 0x16, 0x2e, 0x72, 0x65, 0x73, 0x6f, 0x75, 0x72, 0x63, 0x65,
	0x73, 0x2e, 0x50, 0x65, 0x72, 0x6d, 0x69, 0x73, 0x73, 0x69, 0x6f, 0x6e, 0x73, 0x52, 0x0b, 0x70,
	0x65, 0x72, 0x6d, 0x69, 0x73, 0x73, 0x69, 0x6f, 0x6e, 0x73, 0x22, 0x40, 0x0a, 0x05, 0x4f, 0x77,
	0x6e, 0x65, 0x72, 0x12, 0x1d, 0x0a, 0x0a, 0x63, 0x72, 0x65, 0x61, 0x74, 0x65, 0x64, 0x5f, 0x62,
	0x79, 0x18, 0x01, 0x20, 0x01, 0x28, 0x09, 0x52, 0x09, 0x63, 0x72, 0x65, 0x61, 0x74, 0x65, 0x64,
	0x42, 0x79, 0x12, 0x18, 0x0a, 0x07, 0x70, 0x72, 0x6f, 0x6a, 0x65, 0x63, 0x74, 0x18, 0x02, 0x20,
	0x01, 0x28, 0x09, 0x52, 0x07, 0x70, 0x72, 0x6f, 0x6a, 0x65, 0x63, 0x74, 0x22, 0x69, 0x0a, 0x0b,
	0x50, 0x65, 0x72, 0x6d, 0x69, 0x73, 0x73, 0x69, 0x6f, 0x6e, 0x73, 0x12, 0x2d, 0x0a, 0x06, 0x6d,
	0x79, 0x73, 0x65, 0x6c, 0x66, 0x18, 0x01, 0x20, 0x03, 0x28, 0x0e, 0x32, 0x15, 0x2e, 0x72, 0x65,
	0x73, 0x6f, 0x75, 0x72, 0x63, 0x65, 0x73, 0x2e, 0x50, 0x65, 0x72, 0x6d, 0x69, 0x73, 0x73, 0x69,
	0x6f, 0x6e, 0x52, 0x06, 0x6d, 0x79, 0x73, 0x65, 0x6c, 0x66, 0x12, 0x2b, 0x0a, 0x06, 0x6f, 0x74,
	0x68, 0x65, 0x72, 0x73, 0x18, 0x02, 0x20, 0x03, 0x28, 0x0b, 0x32, 0x13, 0x2e, 0x72, 0x65, 0x73,
	0x6f, 0x75, 0x72, 0x63, 0x65, 0x73, 0x2e, 0x41, 0x63, 0x6c, 0x45, 0x6e, 0x74, 0x72, 0x79, 0x52,
	0x06, 0x6f, 0x74, 0x68, 0x65, 0x72, 0x73, 0x22, 0xa8, 0x02, 0x0a, 0x08, 0x41, 0x63, 0x6c, 0x45,
	0x6e, 0x74, 0x72, 0x79, 0x12, 0x1b, 0x0a, 0x09, 0x65, 0x6e, 0x74, 0x69, 0x74, 0x79, 0x5f, 0x69,
	0x64, 0x18, 0x01, 0x20, 0x01, 0x28, 0x09, 0x52, 0x08, 0x65, 0x6e, 0x74, 0x69, 0x74, 0x79, 0x49,
	0x64, 0x12, 0x25, 0x0a, 0x0e, 0x65, 0x6e, 0x74, 0x69, 0x74, 0x79, 0x5f, 0x70, 0x72, 0x6f, 0x6a,
	0x65, 0x63, 0x74, 0x18, 0x02, 0x20, 0x01, 0x28, 0x09, 0x52, 0x0d, 0x65, 0x6e, 0x74, 0x69, 0x74,
	0x79, 0x50, 0x72, 0x6f, 0x6a, 0x65, 0x63, 0x74, 0x12, 0x3f, 0x0a, 0x0b, 0x65, 0x6e, 0x74, 0x69,
	0x74, 0x79, 0x5f, 0x74, 0x79, 0x70, 0x65, 0x18, 0x03, 0x20, 0x01, 0x28, 0x0e, 0x32, 0x1e, 0x2e,
	0x72, 0x65, 0x73, 0x6f, 0x75, 0x72, 0x63, 0x65, 0x73, 0x2e, 0x41, 0x63, 0x6c, 0x45, 0x6e, 0x74,
	0x72, 0x79, 0x2e, 0x45, 0x6e, 0x74, 0x69, 0x74, 0x79, 0x54, 0x79, 0x70, 0x65, 0x52, 0x0a, 0x65,
	0x6e, 0x74, 0x69, 0x74, 0x79, 0x54, 0x79, 0x70, 0x65, 0x12, 0x37, 0x0a, 0x0b, 0x70, 0x65, 0x72,
	0x6d, 0x69, 0x73, 0x73, 0x69, 0x6f, 0x6e, 0x73, 0x18, 0x04, 0x20, 0x03, 0x28, 0x0e, 0x32, 0x15,
	0x2e, 0x72, 0x65, 0x73, 0x6f, 0x75, 0x72, 0x63, 0x65, 0x73, 0x2e, 0x50, 0x65, 0x72, 0x6d, 0x69,
	0x73, 0x73, 0x69, 0x6f, 0x6e, 0x52, 0x0b, 0x70, 0x65, 0x72, 0x6d, 0x69, 0x73, 0x73, 0x69, 0x6f,
	0x6e, 0x73, 0x22, 0x5e, 0x0a, 0x0a, 0x45, 0x6e, 0x74, 0x69, 0x74, 0x79, 0x54, 0x79, 0x70, 0x65,
	0x12, 0x1b, 0x0a, 0x17, 0x45, 0x4e, 0x54, 0x49, 0x54, 0x59, 0x5f, 0x54, 0x59, 0x50, 0x45, 0x5f,
	0x55, 0x4e, 0x53, 0x50, 0x45, 0x43, 0x49, 0x46, 0x49, 0x45, 0x44, 0x10, 0x00, 0x12, 0x14, 0x0a,
	0x10, 0x45, 0x4e, 0x54, 0x49, 0x54, 0x59, 0x5f, 0x54, 0x59, 0x50, 0x45, 0x5f, 0x55, 0x53, 0x45,
	0x52, 0x10, 0x01, 0x12, 0x1d, 0x0a, 0x19, 0x45, 0x4e, 0x54, 0x49, 0x54, 0x59, 0x5f, 0x54, 0x59,
	0x50, 0x45, 0x5f, 0x50, 0x52, 0x4f, 0x4a, 0x45, 0x43, 0x54, 0x5f, 0x47, 0x52, 0x4f, 0x55, 0x50,
	0x10, 0x02, 0x22, 0x80, 0x01, 0x0a, 0x0a, 0x55, 0x70, 0x64, 0x61, 0x74, 0x65, 0x64, 0x41, 0x63,
	0x6c, 0x12, 0x34, 0x0a, 0x0b, 0x6e, 0x65, 0x77, 0x5f, 0x65, 0x6e, 0x74, 0x72, 0x69, 0x65, 0x73,
	0x18, 0x01, 0x20, 0x03, 0x28, 0x0b, 0x32, 0x13, 0x2e, 0x72, 0x65, 0x73, 0x6f, 0x75, 0x72, 0x63,
	0x65, 0x73, 0x2e, 0x41, 0x63, 0x6c, 0x45, 0x6e, 0x74, 0x72, 0x79, 0x52, 0x0a, 0x6e, 0x65, 0x77,
	0x45, 0x6e, 0x74, 0x72, 0x69, 0x65, 0x73, 0x12, 0x3c, 0x0a, 0x0f, 0x64, 0x65, 0x6c, 0x65, 0x74,
	0x65, 0x64, 0x5f, 0x65, 0x6e, 0x74, 0x72, 0x69, 0x65, 0x73, 0x18, 0x02, 0x20, 0x03, 0x28, 0x0b,
	0x32, 0x13, 0x2e, 0x72, 0x65, 0x73, 0x6f, 0x75, 0x72, 0x63, 0x65, 0x73, 0x2e, 0x41, 0x63, 0x6c,
	0x45, 0x6e, 0x74, 0x72, 0x79, 0x52, 0x0e, 0x64, 0x65, 0x6c, 0x65, 0x74, 0x65, 0x64, 0x45, 0x6e,
	0x74, 0x72, 0x69, 0x65, 0x73, 0x22, 0xdd, 0x04, 0x0a, 0x05, 0x46, 0x6c, 0x61, 0x67, 0x73, 0x12,
	0x25, 0x0a, 0x0e, 0x69, 0x6e, 0x63, 0x6c, 0x75, 0x64, 0x65, 0x5f, 0x6f, 0x74, 0x68, 0x65, 0x72,
	0x73, 0x18, 0x01, 0x20, 0x01, 0x28, 0x08, 0x52, 0x0d, 0x69, 0x6e, 0x63, 0x6c, 0x75, 0x64, 0x65,
	0x4f, 0x74, 0x68, 0x65, 0x72, 0x73, 0x12, 0x27, 0x0a, 0x0f, 0x69, 0x6e, 0x63, 0x6c, 0x75, 0x64,
	0x65, 0x5f, 0x75, 0x70, 0x64, 0x61, 0x74, 0x65, 0x73, 0x18, 0x02, 0x20, 0x01, 0x28, 0x08, 0x52,
	0x0e, 0x69, 0x6e, 0x63, 0x6c, 0x75, 0x64, 0x65, 0x55, 0x70, 0x64, 0x61, 0x74, 0x65, 0x73, 0x12,
	0x27, 0x0a, 0x0f, 0x69, 0x6e, 0x63, 0x6c, 0x75, 0x64, 0x65, 0x5f, 0x73, 0x75, 0x70, 0x70, 0x6f,
	0x72, 0x74, 0x18, 0x03, 0x20, 0x01, 0x28, 0x08, 0x52, 0x0e, 0x69, 0x6e, 0x63, 0x6c, 0x75, 0x64,
	0x65, 0x53, 0x75, 0x70, 0x70, 0x6f, 0x72, 0x74, 0x12, 0x2a, 0x0a, 0x11, 0x66, 0x69, 0x6c, 0x74,
	0x65, 0x72, 0x5f, 0x63, 0x72, 0x65, 0x61, 0x74, 0x65, 0x64, 0x5f, 0x62, 0x79, 0x18, 0x04, 0x20,
	0x01, 0x28, 0x09, 0x52, 0x0f, 0x66, 0x69, 0x6c, 0x74, 0x65, 0x72, 0x43, 0x72, 0x65, 0x61, 0x74,
	0x65, 0x64, 0x42, 0x79, 0x12, 0x30, 0x0a, 0x14, 0x66, 0x69, 0x6c, 0x74, 0x65, 0x72, 0x5f, 0x63,
	0x72, 0x65, 0x61, 0x74, 0x65, 0x64, 0x5f, 0x61, 0x66, 0x74, 0x65, 0x72, 0x18, 0x05, 0x20, 0x01,
	0x28, 0x03, 0x52, 0x12, 0x66, 0x69, 0x6c, 0x74, 0x65, 0x72, 0x43, 0x72, 0x65, 0x61, 0x74, 0x65,
	0x64, 0x41, 0x66, 0x74, 0x65, 0x72, 0x12, 0x32, 0x0a, 0x15, 0x66, 0x69, 0x6c, 0x74, 0x65, 0x72,
	0x5f, 0x63, 0x72, 0x65, 0x61, 0x74, 0x65, 0x64, 0x5f, 0x62, 0x65, 0x66, 0x6f, 0x72, 0x65, 0x18,
	0x06, 0x20, 0x01, 0x28, 0x03, 0x52, 0x13, 0x66, 0x69, 0x6c, 0x74, 0x65, 0x72, 0x43, 0x72, 0x65,
	0x61, 0x74, 0x65, 0x64, 0x42, 0x65, 0x66, 0x6f, 0x72, 0x65, 0x12, 0x2a, 0x0a, 0x11, 0x66, 0x69,
	0x6c, 0x74, 0x65, 0x72, 0x5f, 0x70, 0x72, 0x6f, 0x64, 0x75, 0x63, 0x74, 0x5f, 0x69, 0x64, 0x18,
	0x08, 0x20, 0x01, 0x28, 0x09, 0x52, 0x0f, 0x66, 0x69, 0x6c, 0x74, 0x65, 0x72, 0x50, 0x72, 0x6f,
	0x64, 0x75, 0x63, 0x74, 0x49, 0x64, 0x12, 0x36, 0x0a, 0x17, 0x66, 0x69, 0x6c, 0x74, 0x65, 0x72,
	0x5f, 0x70, 0x72, 0x6f, 0x64, 0x75, 0x63, 0x74, 0x5f, 0x63, 0x61, 0x74, 0x65, 0x67, 0x6f, 0x72,
	0x79, 0x18, 0x09, 0x20, 0x01, 0x28, 0x09, 0x52, 0x15, 0x66, 0x69, 0x6c, 0x74, 0x65, 0x72, 0x50,
	0x72, 0x6f, 0x64, 0x75, 0x63, 0x74, 0x43, 0x61, 0x74, 0x65, 0x67, 0x6f, 0x72, 0x79, 0x12, 0x36,
	0x0a, 0x17, 0x66, 0x69, 0x6c, 0x74, 0x65, 0x72, 0x5f, 0x70, 0x72, 0x6f, 0x64, 0x75, 0x63, 0x74,
	0x5f, 0x70, 0x72, 0x6f, 0x76, 0x69, 0x64, 0x65, 0x72, 0x18, 0x0a, 0x20, 0x01, 0x28, 0x09, 0x52,
	0x15, 0x66, 0x69, 0x6c, 0x74, 0x65, 0x72, 0x50, 0x72, 0x6f, 0x64, 0x75, 0x63, 0x74, 0x50, 0x72,
	0x6f, 0x76, 0x69, 0x64, 0x65, 0x72, 0x12, 0x1d, 0x0a, 0x0a, 0x66, 0x69, 0x6c, 0x74, 0x65, 0x72,
	0x5f, 0x69, 0x64, 0x73, 0x18, 0x0b, 0x20, 0x03, 0x28, 0x03, 0x52, 0x09, 0x66, 0x69, 0x6c, 0x74,
	0x65, 0x72, 0x49, 0x64, 0x73, 0x12, 0x26, 0x0a, 0x0f, 0x68, 0x69, 0x64, 0x65, 0x5f, 0x70, 0x72,
	0x6f, 0x64, 0x75, 0x63, 0x74, 0x5f, 0x69, 0x64, 0x18, 0x0c, 0x20, 0x01, 0x28, 0x09, 0x52, 0x0d,
	0x68, 0x69, 0x64, 0x65, 0x50, 0x72, 0x6f, 0x64, 0x75, 0x63, 0x74, 0x49, 0x64, 0x12, 0x32, 0x0a,
	0x15, 0x68, 0x69, 0x64, 0x65, 0x5f, 0x70, 0x72, 0x6f, 0x64, 0x75, 0x63, 0x74, 0x5f, 0x63, 0x61,
	0x74, 0x65, 0x67, 0x6f, 0x72, 0x79, 0x18, 0x0d, 0x20, 0x01, 0x28, 0x09, 0x52, 0x13, 0x68, 0x69,
	0x64, 0x65, 0x50, 0x72, 0x6f, 0x64, 0x75, 0x63, 0x74, 0x43, 0x61, 0x74, 0x65, 0x67, 0x6f, 0x72,
	0x79, 0x12, 0x32, 0x0a, 0x15, 0x68, 0x69, 0x64, 0x65, 0x5f, 0x70, 0x72, 0x6f, 0x64, 0x75, 0x63,
	0x74, 0x5f, 0x70, 0x72, 0x6f, 0x76, 0x69, 0x64, 0x65, 0x72, 0x18, 0x0e, 0x20, 0x01, 0x28, 0x09,
	0x52, 0x13, 0x68, 0x69, 0x64, 0x65, 0x50, 0x72, 0x6f, 0x64, 0x75, 0x63, 0x74, 0x50, 0x72, 0x6f,
	0x76, 0x69, 0x64, 0x65, 0x72, 0x2a, 0x81, 0x01, 0x0a, 0x0a, 0x50, 0x65, 0x72, 0x6d, 0x69, 0x73,
	0x73, 0x69, 0x6f, 0x6e, 0x12, 0x1a, 0x0a, 0x16, 0x50, 0x45, 0x52, 0x4d, 0x49, 0x53, 0x53, 0x49,
	0x4f, 0x4e, 0x5f, 0x55, 0x4e, 0x53, 0x50, 0x45, 0x43, 0x49, 0x46, 0x49, 0x45, 0x44, 0x10, 0x00,
	0x12, 0x13, 0x0a, 0x0f, 0x50, 0x45, 0x52, 0x4d, 0x49, 0x53, 0x53, 0x49, 0x4f, 0x4e, 0x5f, 0x52,
	0x45, 0x41, 0x44, 0x10, 0x01, 0x12, 0x13, 0x0a, 0x0f, 0x50, 0x45, 0x52, 0x4d, 0x49, 0x53, 0x53,
	0x49, 0x4f, 0x4e, 0x5f, 0x45, 0x44, 0x49, 0x54, 0x10, 0x02, 0x12, 0x14, 0x0a, 0x10, 0x50, 0x45,
	0x52, 0x4d, 0x49, 0x53, 0x53, 0x49, 0x4f, 0x4e, 0x5f, 0x41, 0x44, 0x4d, 0x49, 0x4e, 0x10, 0x03,
	0x12, 0x17, 0x0a, 0x13, 0x50, 0x45, 0x52, 0x4d, 0x49, 0x53, 0x53, 0x49, 0x4f, 0x4e, 0x5f, 0x50,
	0x52, 0x4f, 0x56, 0x49, 0x44, 0x45, 0x52, 0x10, 0x04, 0x42, 0x37, 0x0a, 0x1c, 0x64, 0x6b, 0x2e,
	0x73, 0x64, 0x75, 0x2e, 0x63, 0x6c, 0x6f, 0x75, 0x64, 0x2e, 0x72, 0x65, 0x73, 0x6f, 0x75, 0x72,
	0x63, 0x65, 0x73, 0x2e, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x5a, 0x17, 0x75, 0x63, 0x6c, 0x6f, 0x75,
	0x64, 0x2e, 0x64, 0x6b, 0x2f, 0x61, 0x70, 0x6d, 0x2f, 0x72, 0x65, 0x73, 0x6f, 0x75, 0x72, 0x63,
	0x65, 0x73, 0x62, 0x06, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x33,
}

var (
	file_resources_proto_rawDescOnce sync.Once
	file_resources_proto_rawDescData = file_resources_proto_rawDesc
)

func file_resources_proto_rawDescGZIP() []byte {
	file_resources_proto_rawDescOnce.Do(func() {
		file_resources_proto_rawDescData = protoimpl.X.CompressGZIP(file_resources_proto_rawDescData)
	})
	return file_resources_proto_rawDescData
}

var file_resources_proto_enumTypes = make([]protoimpl.EnumInfo, 2)
var file_resources_proto_msgTypes = make([]protoimpl.MessageInfo, 6)
var file_resources_proto_goTypes = []interface{}{
	(Permission)(0),          // 0: resources.Permission
	(AclEntry_EntityType)(0), // 1: resources.AclEntry.EntityType
	(*Metadata)(nil),         // 2: resources.Metadata
	(*Owner)(nil),            // 3: resources.Owner
	(*Permissions)(nil),      // 4: resources.Permissions
	(*AclEntry)(nil),         // 5: resources.AclEntry
	(*UpdatedAcl)(nil),       // 6: resources.UpdatedAcl
	(*Flags)(nil),            // 7: resources.Flags
}
var file_resources_proto_depIdxs = []int32{
	3, // 0: resources.Metadata.owner:type_name -> resources.Owner
	4, // 1: resources.Metadata.permissions:type_name -> resources.Permissions
	0, // 2: resources.Permissions.myself:type_name -> resources.Permission
	5, // 3: resources.Permissions.others:type_name -> resources.AclEntry
	1, // 4: resources.AclEntry.entity_type:type_name -> resources.AclEntry.EntityType
	0, // 5: resources.AclEntry.permissions:type_name -> resources.Permission
	5, // 6: resources.UpdatedAcl.new_entries:type_name -> resources.AclEntry
	5, // 7: resources.UpdatedAcl.deleted_entries:type_name -> resources.AclEntry
	8, // [8:8] is the sub-list for method output_type
	8, // [8:8] is the sub-list for method input_type
	8, // [8:8] is the sub-list for extension type_name
	8, // [8:8] is the sub-list for extension extendee
	0, // [0:8] is the sub-list for field type_name
}

func init() { file_resources_proto_init() }
func file_resources_proto_init() {
	if File_resources_proto != nil {
		return
	}
	if !protoimpl.UnsafeEnabled {
		file_resources_proto_msgTypes[0].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*Metadata); i {
			case 0:
				return &v.state
			case 1:
				return &v.sizeCache
			case 2:
				return &v.unknownFields
			default:
				return nil
			}
		}
		file_resources_proto_msgTypes[1].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*Owner); i {
			case 0:
				return &v.state
			case 1:
				return &v.sizeCache
			case 2:
				return &v.unknownFields
			default:
				return nil
			}
		}
		file_resources_proto_msgTypes[2].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*Permissions); i {
			case 0:
				return &v.state
			case 1:
				return &v.sizeCache
			case 2:
				return &v.unknownFields
			default:
				return nil
			}
		}
		file_resources_proto_msgTypes[3].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*AclEntry); i {
			case 0:
				return &v.state
			case 1:
				return &v.sizeCache
			case 2:
				return &v.unknownFields
			default:
				return nil
			}
		}
		file_resources_proto_msgTypes[4].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*UpdatedAcl); i {
			case 0:
				return &v.state
			case 1:
				return &v.sizeCache
			case 2:
				return &v.unknownFields
			default:
				return nil
			}
		}
		file_resources_proto_msgTypes[5].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*Flags); i {
			case 0:
				return &v.state
			case 1:
				return &v.sizeCache
			case 2:
				return &v.unknownFields
			default:
				return nil
			}
		}
	}
	type x struct{}
	out := protoimpl.TypeBuilder{
		File: protoimpl.DescBuilder{
			GoPackagePath: reflect.TypeOf(x{}).PkgPath(),
			RawDescriptor: file_resources_proto_rawDesc,
			NumEnums:      2,
			NumMessages:   6,
			NumExtensions: 0,
			NumServices:   0,
		},
		GoTypes:           file_resources_proto_goTypes,
		DependencyIndexes: file_resources_proto_depIdxs,
		EnumInfos:         file_resources_proto_enumTypes,
		MessageInfos:      file_resources_proto_msgTypes,
	}.Build()
	File_resources_proto = out.File
	file_resources_proto_rawDesc = nil
	file_resources_proto_goTypes = nil
	file_resources_proto_depIdxs = nil
}