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
@Table(name = "org")
@XmlRootElement
@NamedQueries({
        @NamedQuery(name = "Org.findAll", query = "SELECT o FROM Org o")
        , @NamedQuery(name = "Org.findById", query = "SELECT o FROM Org o WHERE o.id = :id")
        , @NamedQuery(name = "Org.findByOrgfullname", query = "SELECT o FROM Org o WHERE o.orgfullname = :orgfullname")
        , @NamedQuery(name = "Org.findByOrgshortname", query = "SELECT o FROM Org o WHERE o.orgshortname = :orgshortname")
        , @NamedQuery(name = "Org.findByLastmodified", query = "SELECT o FROM Org o WHERE o.lastmodified = :lastmodified")
        , @NamedQuery(name = "Org.findByOrgactive", query = "SELECT o FROM Org o WHERE o.orgactive = :orgactive")})
public class Org implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Column(name = "orgfullname")
    private String orgfullname;
    @Column(name = "orgshortname")
    private String orgshortname;
    @Basic(optional = false)
    @Column(name = "lastmodified")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastmodified;
    @Column(name = "orgactive")
    private Integer orgactive;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "orgrefid")
    private List<Projectorgrel> projectorgrelList;

    public Org() {
    }

    public Org(Integer id) {
        this.id = id;
    }

    public Org(Integer id, Date lastmodified) {
        this.id = id;
        this.lastmodified = lastmodified;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getOrgfullname() {
        return orgfullname;
    }

    public void setOrgfullname(String orgfullname) {
        this.orgfullname = orgfullname;
    }

    public String getOrgshortname() {
        return orgshortname;
    }

    public void setOrgshortname(String orgshortname) {
        this.orgshortname = orgshortname;
    }

    public Date getLastmodified() {
        return lastmodified;
    }

    public void setLastmodified(Date lastmodified) {
        this.lastmodified = lastmodified;
    }

    public Integer getOrgactive() {
        return orgactive;
    }

    public void setOrgactive(Integer orgactive) {
        this.orgactive = orgactive;
    }

    @XmlTransient
    public List<Projectorgrel> getProjectorgrelList() {
        return projectorgrelList;
    }

    public void setProjectorgrelList(List<Projectorgrel> projectorgrelList) {
        this.projectorgrelList = projectorgrelList;
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
        if (!(object instanceof Org)) {
            return false;
        }
        Org other = (Org) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.escience.jpa.escienceclouddb.Org[ id=" + id + " ]";
    }

}
