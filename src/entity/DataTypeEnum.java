package entity;

public enum DataTypeEnum {
	COMPLETE,//complete data
	NULL_EQUALITY,//incomplete data that treats a null value equal to other null values
	NULL_UNCERTAINTY//incomplete data that treats a null value as an uncertain value
}
