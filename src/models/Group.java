package models;


public class Group {
	private int id;
	private String groupName;
	private int groupThumnail;
	private String ownerAccount;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getGroupName() {
		return groupName;
	}
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	public int getGroupThumnail() {
		return groupThumnail;
	}
	public void setGroupThumnail(int groupThumnail) {
		this.groupThumnail = groupThumnail;
	}
	public String getOwnerAccount() {
		return ownerAccount;
	}
	public void setOwnerAccount(String ownerAccount) {
		this.ownerAccount = ownerAccount;
	}
	
	
}
