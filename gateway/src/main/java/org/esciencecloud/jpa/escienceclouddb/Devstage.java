/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esciencecloud.jpa.escienceclouddb;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author bjhj
 */
@Entity
@Table(name = "devstage")
@XmlRootElement
@NamedQueries({
        @NamedQuery(name = "Devstage.findAll", query = "SELECT d FROM Devstage d")
        , @NamedQuery(name = "Devstage.findById", query = "SELECT d FROM Devstage d WHERE d.id = :id")
        , @NamedQuery(name = "Devstage.findByDevstagetext", query = "SELECT d FROM Devstage d WHERE d.devstagetext = :devstagetext")
        , @NamedQuery(name = "Devstage.findByLastmodified", query = "SELECT d FROM Devstage d WHERE d.lastmodified = :lastmodified")})
public class Devstage implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Column(name = "devstagetext")
    private String devstagetext;
    @Basic(optional = false)
    @Column(name = "lastmodified")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastmodified;
    @OneToMany(mappedBy = "devstagerefid")
    private List<Software> softwareList;

    public Devstage() {
    }

    public Devstage(Integer id) {
        this.id = id;
    }

    public Devstage(Integer id, Date lastmodified) {
        this.id = id;
        this.lastmodified = lastmodified;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDevstagetext() {
        return devstagetext;
    }

    public void setDevstagetext(String devstagetext) {
        this.devstagetext = devstagetext;
    }

    public Date getLastmodified() {
        return lastmodified;
    }

    public void setLastmodified(Date lastmodified) {
        this.lastmodified = lastmodified;
    }

    @XmlTransient
    public List<Software> getSoftwareList() {
        return softwareList;
    }

    public void setSoftwareList(List<Software> softwareList) {
        this.softwareList = softwareList;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Devstage)) {
            return false;
        }
        Devstage other = (Devstage) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.escience.jpa.escienceclouddb.Devstage[ id=" + id + " ]";
    }

}
