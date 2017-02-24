package com.wenyi.mafia;

import android.os.Parcel;
import android.os.Parcelable;

public class RoomInfo implements Parcelable {
	String roomName,roomMaster,roleMsg;
	int mens,useAudio,roomID,current,gods,wolfs,saveSelfDays,lastWordDays,hasPassword,gaming;
	public RoomInfo() {
		// TODO Auto-generated constructor stub
	}
	public RoomInfo(String name,String master,String roleMsg,int mens,int useAudio,int roomID,int current,int gods,int wolfs,
					int saveSelfDays,int lastWordDays,int hasPassword,int gaming) {
		// TODO Auto-generated constructor stub
		super();
		this.roomName = name;
		this.roomMaster = master;
		this.roleMsg = roleMsg;
		this.mens = mens;
		this.useAudio = useAudio;
		this.roomID = roomID;
		this.current = current;
		this.gods = gods;
		this.wolfs = wolfs;
		this.saveSelfDays = saveSelfDays;
		this.lastWordDays = lastWordDays;
		this.hasPassword = hasPassword;
		this.gaming = gaming;
	}
	public String getRoomName() {
		return roomName;
	}
	public void setRoomName(String roomName) {
		this.roomName = roomName;
	}
	public int getMens() {
		return mens;
	}
	public void setMens(int mens) {
		this.mens = mens;
	}
	public int getUseAudio() {
		return useAudio;
	}
	public void setUseAudio(int useAudio) {
		this.useAudio = useAudio;
	}
	public String getRoomMaster() {
		return roomMaster;
	}
	public void setRoomMaster(String roomMaster) {
		this.roomMaster = roomMaster;
	}
	public int getRoomID() {
		return roomID;
	}
	public void setRoomID(int roomID) {
		this.roomID = roomID;
	}
	public int getCurrent() {
		return current;
	}
	public void setCurrent(int current) {
		this.current = current;
	}
	public String getRoleMsg() {
		return roleMsg;
	}
	public void setRoleMsg(String roleMsg) {
		this.roleMsg = roleMsg;
	}
	public int getGods() {
		return gods;
	}
	public void setGods(int gods) {
		this.gods = gods;
	}
	public int getWolfs() {
		return wolfs;
	}
	public void setWolfs(int wolfs) {
		this.wolfs = wolfs;
	}
	public int getSaveSelfDays() {
		return saveSelfDays;
	}
	public void setSaveSelfDays(int saveSelfDays) {
		this.saveSelfDays = saveSelfDays;
	}
	public int getLastWordDays() {
		return lastWordDays;
	}
	public void setLastWordDays(int lastWordDays) {
		this.lastWordDays = lastWordDays;
	}
	public int getHasPassword() {
		return hasPassword;
	}
	public void setHasPassword(int hasPassword) {
		this.hasPassword = hasPassword;
	}
	public int getGaming() {
		return gaming;
	}
	public void setGaming(int gaming) {
		this.gaming = gaming;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int i) {
		parcel.writeInt(mens);
		parcel.writeInt(useAudio);
		parcel.writeInt(roomID);
		parcel.writeInt(current);
		parcel.writeInt(gods);
		parcel.writeInt(wolfs);
		parcel.writeString(roomMaster);
		parcel.writeString(roomName);
		parcel.writeString(roleMsg);
		parcel.writeInt(saveSelfDays);
		parcel.writeInt(lastWordDays);
		parcel.writeInt(hasPassword);
		parcel.writeInt(gaming);
	}
	public static final Parcelable.Creator<RoomInfo> CREATOR = new Creator<RoomInfo>(){

		@Override
		public RoomInfo createFromParcel(Parcel parcel) {
			return new RoomInfo(parcel);
		}

		@Override
		public RoomInfo[] newArray(int i) {
			return new RoomInfo[0];
		}
	};
	public RoomInfo(Parcel parcel){
		mens = parcel.readInt();
		useAudio = parcel.readInt();
		roomID = parcel.readInt();
		current = parcel.readInt();
		gods = parcel.readInt();
		wolfs = parcel.readInt();
		roomMaster = parcel.readString();
		roomName = parcel.readString();
		roleMsg = parcel.readString();
		saveSelfDays = parcel.readInt();
		lastWordDays = parcel.readInt();
		hasPassword = parcel.readInt();
		gaming = parcel.readInt();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RoomInfo){
			RoomInfo info = (RoomInfo)obj;
			return (wolfs == info.getWolfs() && gods == info.getGods() && roomName.equals(info.getRoomName()) && roleMsg.equals(info.getRoleMsg()) &&
					hasPassword == info.getHasPassword() && saveSelfDays == info.getSaveSelfDays() && lastWordDays == info.getLastWordDays() &&
					mens == info.getMens() && roomID == info.getRoomID());
		}else return false;
//		return super.equals(obj);
	}
}
